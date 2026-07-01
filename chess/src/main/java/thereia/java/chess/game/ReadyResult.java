package thereia.java.chess.game;

import lombok.Getter;
import thereia.java.chess.protocol.GameStartMessage;

@Getter
public final class ReadyResult {

    private final boolean started;
    private final GameStartMessage gameStartRed;
    private final GameStartMessage gameStartBlack;

    public ReadyResult(boolean started, GameStartMessage gameStartRed, GameStartMessage gameStartBlack) {
        this.started = started;
        this.gameStartRed = gameStartRed;
        this.gameStartBlack = gameStartBlack;
    }
}
