package thereia.java.chess.board;

import thereia.java.chess.piece.Piece;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class Board {

    private final Map<Position, Piece> pieces;

    private Board(Map<Position, Piece> pieces) {
        this.pieces = Map.copyOf(pieces);
    }

    public static Board empty() {
        return new Board(Collections.emptyMap());
    }

    public static Board initial() {
        throw new UnsupportedOperationException("Board.initial is implemented in Task 3");
    }

    public Optional<Piece> pieceAt(Position position) {
        return Optional.ofNullable(pieces.get(position));
    }

    public boolean isEmpty(Position position) {
        return !pieces.containsKey(position);
    }

    public int occupiedCount() {
        return pieces.size();
    }

    public int countBetween(Position from, Position to) {
        throw new UnsupportedOperationException("Board.countBetween is implemented in Task 3");
    }

    public Board move(Position from, Position to, Piece movedPiece) {
        Map<Position, Piece> next = new HashMap<>(pieces);
        next.remove(from);
        next.put(to, movedPiece);
        return new Board(next);
    }

    public Board remove(Position position) {
        Map<Position, Piece> next = new HashMap<>(pieces);
        next.remove(position);
        return new Board(next);
    }

    public Board put(Position position, Piece piece) {
        Map<Position, Piece> next = new HashMap<>(pieces);
        next.put(position, piece);
        return new Board(next);
    }
}
