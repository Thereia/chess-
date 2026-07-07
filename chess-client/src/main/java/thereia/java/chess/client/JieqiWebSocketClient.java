package thereia.java.chess.client;

import java.net.URI;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JieqiWebSocketClient extends WebSocketClient {

    private static final Logger log = Logger.getLogger(JieqiWebSocketClient.class.getName());

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Consumer<JsonNode> messageHandler;
    private Consumer<Boolean> connectionHandler;
    private Timer reconnectTimer;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final int RECONNECT_DELAY_MS = 3000;

    public JieqiWebSocketClient(URI serverUri) {
        super(serverUri);
        log.info("Created WebSocket client for: " + serverUri);
        setConnectionLostTimeout(0);
        setTcpNoDelay(true);
    }

    public void setMessageHandler(Consumer<JsonNode> handler) {
        this.messageHandler = handler;
    }

    public void setConnectionHandler(Consumer<Boolean> handler) {
        this.connectionHandler = handler;
    }

    public void sendMessage(String messageType, ObjectNode payload) {
        try {
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("messageType", messageType);

            if (payload != null) {
                rootNode.setAll(payload);
            }

            String message = rootNode.toString();
            log.fine("Sending message: " + message);
            send(message);
        } catch (Exception e) {
            log.warning("Failed to send message: " + e.getMessage());
        }
    }

    public void sendRawMessage(String message) {
        try {
            log.fine("Sending raw message: " + message);
            send(message);
        } catch (Exception e) {
            log.warning("Failed to send raw message: " + e.getMessage());
        }
    }

    public void sendPing() {
        sendMessage("ping", null);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("WebSocket connection opened. HTTP status: " + handshakedata.getHttpStatus());
        reconnectAttempts = 0;
        stopReconnectTimer();
        if (connectionHandler != null) {
            connectionHandler.accept(true);
        }
    }

    @Override
    public void onMessage(String message) {
        log.fine("Received message from server: " + message);
        try {
            JsonNode node = objectMapper.readTree(message);
            if (messageHandler != null) {
                messageHandler.accept(node);
            }
        } catch (Exception e) {
            log.warning("Failed to parse message: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("WebSocket connection closed: code=" + code + ", reason=" + reason);
        if (connectionHandler != null) {
            connectionHandler.accept(false);
        }
        startReconnectTimer();
    }

    @Override
    public void onError(Exception ex) {
        log.warning("WebSocket error: " + ex.getMessage());
        if (connectionHandler != null) {
            connectionHandler.accept(false);
        }
    }

    private void startReconnectTimer() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            log.warning("Max reconnect attempts reached, stopping");
            return;
        }

        stopReconnectTimer();
        reconnectTimer = new Timer();
        reconnectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isOpen()) {
                    reconnectAttempts++;
                    log.info("Attempting reconnect (" + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS + ")...");
                    try {
                        reconnect();
                    } catch (Exception e) {
                        log.warning("Reconnect failed: " + e.getMessage());
                        startReconnectTimer();
                    }
                }
            }
        }, RECONNECT_DELAY_MS);
    }

    private void stopReconnectTimer() {
        if (reconnectTimer != null) {
            reconnectTimer.cancel();
            reconnectTimer = null;
        }
    }

    public void stopReconnect() {
        stopReconnectTimer();
        reconnectAttempts = 0;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }
}