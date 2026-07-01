package thereia.java.chess.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import thereia.java.chess.game.GameRoom;
import thereia.java.chess.game.MatchResult;
import thereia.java.chess.game.ReadyResult;
import thereia.java.chess.game.RoomManager;
import thereia.java.chess.protocol.ErrorMessage;
import thereia.java.chess.protocol.MatchSuccessMessage;
import thereia.java.chess.protocol.MessageType;

import java.io.IOException;

@Component
public final class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SessionRegistry sessionRegistry;
    private final RoomManager roomManager;

    public GameWebSocketHandler(SessionRegistry sessionRegistry, RoomManager roomManager) {
        this.sessionRegistry = sessionRegistry;
        this.roomManager = roomManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            MessageEnvelope envelope = objectMapper.readValue(message.getPayload(), MessageEnvelope.class);
            if (MessageType.startMatch.name().equals(envelope.getMessageType())) {
                handleStartMatch(session);
                return;
            }
            if (MessageType.Ready.name().equals(envelope.getMessageType())) {
                handleReady(session);
                return;
            }
            send(session, ErrorMessage.of(4002, "unknown messageType"));
        } catch (IOException exception) {
            try {
                send(session, ErrorMessage.of(4001, "json format error"));
            } catch (IOException ioException) {
                throw new IllegalStateException("failed to send error message", ioException);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.remove(session.getId());
    }

    private void handleStartMatch(WebSocketSession session) throws IOException {
        MatchResult result = roomManager.startMatch(session.getId());
        if (!result.isMatched()) {
            return;
        }

        GameRoom room = result.getRoom().orElseThrow();
        MatchSuccessMessage message = new MatchSuccessMessage(MessageType.matchSuccess.name(), room.getRoomId());
        send(sessionRegistry.find(room.getRedPlayer().getPlayerId()).orElseThrow(), message);
        send(sessionRegistry.find(room.getBlackPlayer().getPlayerId()).orElseThrow(), message);
    }

    private void handleReady(WebSocketSession session) throws IOException {
        GameRoom room = roomManager.roomForPlayer(session.getId()).orElseThrow();
        ReadyResult result = room.ready(session.getId(), java.time.Instant.now());
        if (!result.isStarted()) {
            return;
        }
        send(sessionRegistry.find(room.getRedPlayer().getPlayerId()).orElseThrow(), result.getGameStartRed());
        send(sessionRegistry.find(room.getBlackPlayer().getPlayerId()).orElseThrow(), result.getGameStartBlack());
    }

    private void send(WebSocketSession session, Object payload) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    public static final class MessageEnvelope {
        private String messageType;

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }
    }
}
