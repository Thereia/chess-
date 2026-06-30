package thereia.java.chess.game;

import lombok.Getter;
import thereia.java.chess.move.Move;
import thereia.java.chess.protocol.GameOverMessage;

import java.time.Instant;

@Getter
public final class GameRoom {

    private final String roomId;
    private final Player redPlayer;
    private final Player blackPlayer;
    private final GameState state;

    public GameRoom(String roomId, Player redPlayer, Player blackPlayer, GameState state) {
        this.roomId = roomId;
        this.redPlayer = redPlayer;
        this.blackPlayer = blackPlayer;
        this.state = state;
    }

    public RoomMoveResult handleMove(String playerId, Move move, Instant now) {
        throw new UnsupportedOperationException("GameRoom.handleMove is implemented in Task 9");
    }

    public GameOverMessage resign(String playerId) {
        throw new UnsupportedOperationException("GameRoom.resign is implemented in Task 9");
    }
}
