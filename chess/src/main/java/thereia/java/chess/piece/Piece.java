package thereia.java.chess.piece;

import java.util.Optional;

public record Piece(
        String id,
        ChessColor color,
        PieceType originalType,
        PieceType actualRevealedType,
        boolean visible
) {

    public Piece {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (color == null) {
            throw new IllegalArgumentException("color must not be null");
        }
        if (originalType == null) {
            throw new IllegalArgumentException("originalType must not be null");
        }
        if (visible && actualRevealedType == null) {
            throw new IllegalArgumentException("visible piece must have revealed type");
        }
        if (!visible && actualRevealedType != null) {
            throw new IllegalArgumentException("hidden piece must not have revealed type");
        }
    }

    public static Piece visible(String id, ChessColor color, PieceType type) {
        return new Piece(id, color, type, type, true);
    }

    public static Piece hidden(String id, ChessColor color, PieceType originalType) {
        return new Piece(id, color, originalType, null, false);
    }

    public PieceType movementType() {
        return visible ? actualRevealedType : originalType;
    }

    public Optional<PieceType> revealedType() {
        return Optional.ofNullable(actualRevealedType);
    }

    public Piece reveal(PieceType type) {
        if (visible) {
            throw new IllegalStateException("piece is already visible");
        }
        if (type == null) {
            throw new IllegalArgumentException("revealed type must not be null");
        }
        return new Piece(id, color, originalType, type, true);
    }
}
