package thereia.java.chess.rule;

import lombok.Getter;
import thereia.java.chess.game.GameState;
import thereia.java.chess.move.Move;
import thereia.java.chess.move.MoveRecord;
import thereia.java.chess.move.MoveValidationResult;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.PieceType;

import java.time.Instant;

public final class MoveExecutor {

    public MoveExecution apply(GameState state, Move move, ChessColor moverColor, Instant now) {
        throw new UnsupportedOperationException("MoveExecutor.apply is implemented in Task 6");
    }

    @Getter
    public static final class MoveExecution {

        private final boolean success;
        private final MoveValidationResult validation;
        private final GameState state;
        private final MoveRecord record;
        private final PieceType flipResult;
        private final PieceType capturedPiece;

        public MoveExecution(boolean success, MoveValidationResult validation, GameState state, MoveRecord record,
                             PieceType flipResult, PieceType capturedPiece) {
            this.success = success;
            this.validation = validation;
            this.state = state;
            this.record = record;
            this.flipResult = flipResult;
            this.capturedPiece = capturedPiece;
        }

    }
}
