package thereia.java.chess.game;

import thereia.java.chess.board.Board;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.FlipPool;
import thereia.java.chess.record.GameRecorder;
import thereia.java.chess.rule.MoveExecutor;
import thereia.java.chess.rule.RuleEngine;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

public final class RoomManager {

    private final RuleEngine ruleEngine;
    private final MoveExecutor moveExecutor;
    private final GameRecorder gameRecorder;

    private String waitingSessionId;
    private GameRoom activeRoom;

    public RoomManager(RuleEngine ruleEngine, MoveExecutor moveExecutor, GameRecorder gameRecorder) {
        this.ruleEngine = ruleEngine;
        this.moveExecutor = moveExecutor;
        this.gameRecorder = gameRecorder;
    }

    public MatchResult startMatch(String sessionId) {
        if (activeRoom != null && roomForPlayer(sessionId).isPresent()) {
            return new MatchResult(true, null, Optional.of(activeRoom));
        }
        if (waitingSessionId == null) {
            waitingSessionId = sessionId;
            return new MatchResult(false, sessionId, Optional.empty());
        }
        if (waitingSessionId.equals(sessionId)) {
            return new MatchResult(false, sessionId, Optional.empty());
        }

        activeRoom = new GameRoom("room-1", new Player(waitingSessionId, ChessColor.RED),
                new Player(sessionId, ChessColor.BLACK), initialPreparingState(),
                ruleEngine, moveExecutor, gameRecorder);
        waitingSessionId = null;
        return new MatchResult(true, null, Optional.of(activeRoom));
    }

    public Optional<GameRoom> roomForPlayer(String sessionId) {
        if (activeRoom == null) {
            return Optional.empty();
        }
        if (activeRoom.getRedPlayer().getPlayerId().equals(sessionId)
                || activeRoom.getBlackPlayer().getPlayerId().equals(sessionId)) {
            return Optional.of(activeRoom);
        }
        return Optional.empty();
    }

    private GameState initialPreparingState() {
        Instant now = Instant.now();
        return new GameState(Board.initial(), null, GameStatus.PREPARING, FlipPool.initial(new Random()),
                FlipPool.initial(new Random()), 0, 0, now, now.plusSeconds(60), null, null);
    }
}
