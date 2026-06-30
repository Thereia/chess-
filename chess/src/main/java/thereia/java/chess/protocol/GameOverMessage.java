package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class GameOverMessage {

    private final String messageType;
    private final String winnerColor;
    private final String reason;

    public GameOverMessage(String messageType, String winnerColor, String reason) {
        this.messageType = messageType;
        this.winnerColor = winnerColor;
        this.reason = reason;
    }

}
