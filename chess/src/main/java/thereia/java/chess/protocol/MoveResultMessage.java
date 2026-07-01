package thereia.java.chess.protocol;

import lombok.Getter;

@Getter
public final class MoveResultMessage {

    private final String messageType;
    private final boolean success;
    private final MoveMessage move;
    private final String flipResult;
    private final boolean valid;
    private final int code;
    private final String message;
    private final String capturedPiece;

    public MoveResultMessage(String messageType, boolean success, MoveMessage move, String flipResult, boolean valid,
                             int code, String message,
                             String capturedPiece) {
        this.messageType = messageType;
        this.success = success;
        this.move = move;
        this.flipResult = flipResult;
        this.valid = valid;
        this.code = code;
        this.message = message;
        this.capturedPiece = capturedPiece;
    }

    @Getter
    public static final class MoveMessage {

        private final String fromX;
        private final int fromY;
        private final String toX;
        private final int toY;
        private final boolean isFlip;

        public MoveMessage(String fromX, int fromY, String toX, int toY, boolean isFlip) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.isFlip = isFlip;
        }
    }
}
