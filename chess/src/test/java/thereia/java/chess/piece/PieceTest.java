package thereia.java.chess.piece;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PieceTest {

    @Test
    void hiddenPieceUsesOriginalTypeUntilRevealed() {
        Piece piece = Piece.hidden("r-a0", ChessColor.RED, PieceType.ROOK);
        assertThat(piece.visible()).isFalse();
        assertThat(piece.movementType()).isEqualTo(PieceType.ROOK);

        Piece revealed = piece.reveal(PieceType.PAWN);
        assertThat(revealed.visible()).isTrue();
        assertThat(revealed.revealedType()).contains(PieceType.PAWN);
        assertThat(revealed.movementType()).isEqualTo(PieceType.PAWN);
    }

    @Test
    void visiblePieceUsesItsVisibleType() {
        Piece piece = Piece.visible("r-e0", ChessColor.RED, PieceType.KING);

        assertThat(piece.visible()).isTrue();
        assertThat(piece.originalType()).isEqualTo(PieceType.KING);
        assertThat(piece.revealedType()).contains(PieceType.KING);
        assertThat(piece.movementType()).isEqualTo(PieceType.KING);
    }

    @Test
    void colorKnowsOpponentAndForwardDirection() {
        assertThat(ChessColor.RED.opponent()).isEqualTo(ChessColor.BLACK);
        assertThat(ChessColor.BLACK.opponent()).isEqualTo(ChessColor.RED);
        assertThat(ChessColor.RED.forwardDy()).isEqualTo(1);
        assertThat(ChessColor.BLACK.forwardDy()).isEqualTo(-1);
    }

    @Test
    void cannotRevealAlreadyVisiblePiece() {
        Piece piece = Piece.visible("r-e0", ChessColor.RED, PieceType.KING);

        assertThatThrownBy(() -> piece.reveal(PieceType.ROOK)).isInstanceOf(IllegalStateException.class);
    }
}
