package thereia.java.chess.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import thereia.java.chess.board.Position;
import thereia.java.chess.game.GameRoom;
import thereia.java.chess.game.MatchResult;
import thereia.java.chess.game.ReadyResult;
import thereia.java.chess.game.RoomMoveResult;
import thereia.java.chess.game.RoomManager;
import thereia.java.chess.move.Move;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.protocol.ErrorMessage;
import thereia.java.chess.protocol.GameOverMessage;
import thereia.java.chess.protocol.MatchSuccessMessage;
import thereia.java.chess.protocol.MessageType;
import thereia.java.chess.protocol.RoomInfoMessage;
import thereia.java.chess.protocol.TimeoutMessage;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public final class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SessionRegistry sessionRegistry;
    private final RoomManager roomManager;
    private final TimeoutScheduler timeoutScheduler;
    private final Map<String, ScheduledFuture<?>> roomTimeoutTasks = new ConcurrentHashMap<>();

    public GameWebSocketHandler(SessionRegistry sessionRegistry, RoomManager roomManager) {
        this(sessionRegistry, roomManager, new DefaultTimeoutScheduler());
    }

    @Autowired
    public GameWebSocketHandler(SessionRegistry sessionRegistry, RoomManager roomManager,
                                TimeoutScheduler timeoutScheduler) {
        this.sessionRegistry = sessionRegistry;
        this.roomManager = roomManager;
        this.timeoutScheduler = timeoutScheduler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            MessageEnvelope envelope = objectMapper.treeToValue(payload, MessageEnvelope.class);
            if (MessageType.startMatch.name().equals(envelope.getMessageType())) {
                handleStartMatch(session);
                return;
            }
            if (MessageType.Ready.name().equals(envelope.getMessageType())) {
                handleReady(session);
                return;
            }
            if (MessageType.move.name().equals(envelope.getMessageType())) {
                handleMove(session, objectMapper.treeToValue(payload, MoveRequest.class));
                return;
            }
            if (MessageType.Resign.name().equals(envelope.getMessageType())) {
                handleResign(session);
                return;
            }
            send(session, ErrorMessage.of(4002, "unknown messageType"));
        } catch (IOException exception) {
            try {
                send(session, ErrorMessage.of(4001, "json format error"));
            } catch (IOException ioException) {
                throw new IllegalStateException("failed to send error message", ioException);
            }
        } catch (RuntimeException exception) {
            try {
                send(session, ErrorMessage.of(5000, exception.getMessage()));
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
        send(sessionRegistry.find(room.getRedPlayer().getPlayerId()).orElseThrow(),
                new MatchSuccessMessage(MessageType.matchSuccess.name(), room.getRoomId(),
                        room.getBlackPlayer().getPlayerId(), room.getBlackPlayer().getPlayerId()));
        send(sessionRegistry.find(room.getBlackPlayer().getPlayerId()).orElseThrow(),
                new MatchSuccessMessage(MessageType.matchSuccess.name(), room.getRoomId(),
                        room.getRedPlayer().getPlayerId(), room.getRedPlayer().getPlayerId()));
    }

    private void handleReady(WebSocketSession session) throws IOException {
        Optional<GameRoom> room = roomForPlayer(session);
        if (room.isEmpty()) {
            return;
        }
        GameRoom gameRoom = room.orElseThrow();
        ReadyResult result = gameRoom.ready(session.getId(), java.time.Instant.now());
        if (!result.isStarted()) {
            RoomInfoMessage roomInfo = result.getRoomInfo();
            if (roomInfo != null) {
                send(opponentSession(gameRoom, session.getId()), roomInfo);
            }
            return;
        }
        send(sessionRegistry.find(gameRoom.getRedPlayer().getPlayerId()).orElseThrow(), result.getGameStartRed());
        send(sessionRegistry.find(gameRoom.getBlackPlayer().getPlayerId()).orElseThrow(), result.getGameStartBlack());
        rescheduleTimeout(gameRoom);
    }

    private void handleMove(WebSocketSession session, MoveRequest request) throws IOException {
        Optional<GameRoom> room = roomForPlayer(session);
        if (room.isEmpty()) {
            return;
        }
        Move move = new Move(Position.of(request.getFromX(), request.getFromY()),
                Position.of(request.getToX(), request.getToY()), request.isFlip());
        RoomMoveResult result = room.orElseThrow().handleMove(session.getId(), move, Instant.now());
        if (!result.isSuccess()) {
            send(session, result.getMoveResult());
            return;
        }

        GameRoom gameRoom = room.orElseThrow();
        sendToRoom(gameRoom, result.getMoveResult());
        if (result.getGameOver() != null) {
            cancelTimeout(gameRoom.getRoomId());
            sendToRoom(gameRoom, result.getGameOver());
            return;
        }
        rescheduleTimeout(gameRoom);
    }

    private void handleResign(WebSocketSession session) throws IOException {
        Optional<GameRoom> room = roomForPlayer(session);
        if (room.isEmpty()) {
            return;
        }
        GameRoom gameRoom = room.orElseThrow();
        GameOverMessage gameOver = gameRoom.resign(session.getId());
        cancelTimeout(gameRoom.getRoomId());
        sendToRoom(gameRoom, gameOver);
    }

    private Optional<GameRoom> roomForPlayer(WebSocketSession session) throws IOException {
        Optional<GameRoom> room = roomManager.roomForPlayer(session.getId());
        if (room.isEmpty()) {
            send(session, ErrorMessage.of(3001, "room not found"));
        }
        return room;
    }

    private void sendToRoom(GameRoom room, Object payload) throws IOException {
        send(sessionRegistry.find(room.getRedPlayer().getPlayerId()).orElseThrow(), payload);
        send(sessionRegistry.find(room.getBlackPlayer().getPlayerId()).orElseThrow(), payload);
    }

    private WebSocketSession opponentSession(GameRoom room, String playerId) {
        String opponentId = room.getRedPlayer().getPlayerId().equals(playerId)
                ? room.getBlackPlayer().getPlayerId()
                : room.getRedPlayer().getPlayerId();
        return sessionRegistry.find(opponentId).orElseThrow();
    }

    private void rescheduleTimeout(GameRoom room) {
        cancelTimeout(room.getRoomId());
        Instant deadline = room.getState().getTurnDeadlineAt();
        ChessColor expiredColor = room.getState().getCurrentTurn();
        long delayMillis = Math.max(0, Duration.between(Instant.now(), deadline).toMillis());
        ScheduledFuture<?> future = timeoutScheduler.schedule(
                () -> handleTimeout(room, expiredColor, deadline),
                delayMillis, TimeUnit.MILLISECONDS);
        roomTimeoutTasks.put(room.getRoomId(), future);
    }

    private void cancelTimeout(String roomId) {
        ScheduledFuture<?> existing = roomTimeoutTasks.remove(roomId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    private void handleTimeout(GameRoom room, ChessColor expiredColor, Instant deadline) {
        try {
            TimeoutMessage timeoutMessage = room.timeout(expiredColor, deadline);
            if (timeoutMessage == null) {
                return;
            }
            cancelTimeout(room.getRoomId());
            sendToRoom(room, timeoutMessage);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to send timeout message", exception);
        }
    }

    private void send(WebSocketSession session, Object payload) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class MessageEnvelope {
        private String messageType;

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class MoveRequest {
        private String messageType;
        private String fromX;
        private int fromY;
        private String toX;
        private int toY;
        private boolean isFlip;

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }

        public String getFromX() {
            return fromX;
        }

        public void setFromX(String fromX) {
            this.fromX = fromX;
        }

        public int getFromY() {
            return fromY;
        }

        public void setFromY(int fromY) {
            this.fromY = fromY;
        }

        public String getToX() {
            return toX;
        }

        public void setToX(String toX) {
            this.toX = toX;
        }

        public int getToY() {
            return toY;
        }

        public void setToY(int toY) {
            this.toY = toY;
        }

        public boolean isFlip() {
            return isFlip;
        }

        public void setIsFlip(boolean flip) {
            isFlip = flip;
        }
    }
}
