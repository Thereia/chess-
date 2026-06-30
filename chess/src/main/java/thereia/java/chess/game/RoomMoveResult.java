package thereia.java.chess.game;

import lombok.Getter;
import thereia.java.chess.protocol.GameOverMessage;
import thereia.java.chess.protocol.MoveResultMessage;

@Getter
public final class RoomMoveResult {

    private final boolean success;
    private final MoveResultMessage moveResult;
    private final GameOverMessage gameOver;

    public RoomMoveResult(boolean success, MoveResultMessage moveResult, GameOverMessage gameOver) {
        this.success = success;
        this.moveResult = moveResult;
        this.gameOver = gameOver;
    }

}
