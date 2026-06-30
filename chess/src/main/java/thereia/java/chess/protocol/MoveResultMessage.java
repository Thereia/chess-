package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class MoveResultMessage {

    private final String messageType;
    private final boolean success;
    private final boolean valid;
    private final int code;
    private final String message;
    private final String fromX;
    private final int fromY;
    private final String toX;
    private final int toY;
    private final String flipResult;
    private final String capturedPiece;
    private final String nextTurn;

    public MoveResultMessage(String messageType, boolean success, boolean valid, int code, String message,
                             String fromX, int fromY, String toX, int toY, String flipResult,
                             String capturedPiece, String nextTurn) {
        this.messageType = messageType;
        this.success = success;
        this.valid = valid;
        this.code = code;
        this.message = message;
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
        this.flipResult = flipResult;
        this.capturedPiece = capturedPiece;
        this.nextTurn = nextTurn;
    }

}
