package thereia.java.chess.protocol;

import lombok.Getter;

import java.util.List;

@Getter
public final class GameStartMessage {

    private final String messageType;
    private final String roomId;
    private final String playerColor;
    private final String firstTurn;
    private final List<InitialPieceMessage> initialBoard;

    public GameStartMessage(String messageType, String roomId, String playerColor, String firstTurn,
                            List<InitialPieceMessage> initialBoard) {
        this.messageType = messageType;
        this.roomId = roomId;
        this.playerColor = playerColor;
        this.firstTurn = firstTurn;
        this.initialBoard = initialBoard;
    }

    @Getter
    public static final class InitialPieceMessage {

        private final String x;
        private final int y;
        private final String color;
        private final String piece;
        private final boolean visible;

        public InitialPieceMessage(String x, int y, String color, String piece, boolean visible) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.piece = piece;
            this.visible = visible;
        }

    }
}
