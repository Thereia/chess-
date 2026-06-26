package thereia.java.chess.board;

public record Position(String x, int y) {

    public Position {
        validateProtocolCoordinate(x, y);
    }

    public static Position of(String x, int y) {
        return new Position(x, y);
    }

    public static Position fromArrayIndex(int row, int col) {
        if (row < 0 || row > 9) {
            throw new IllegalArgumentException("row must be between 0 and 9");
        }
        if (col < 0 || col > 8) {
            throw new IllegalArgumentException("col must be between 0 and 8");
        }
        return new Position(String.valueOf((char) ('a' + col)), 9 - row);
    }

    public int row() {
        return 9 - y;
    }

    public int col() {
        return x.charAt(0) - 'a';
    }

    public int deltaX(Position other) {
        return other.col() - col();
    }

    public int deltaY(Position other) {
        return other.y() - y;
    }

    private static void validateProtocolCoordinate(String x, int y) {
        if (x == null || x.length() != 1) {
            throw new IllegalArgumentException("x must be one character from a to i");
        }
        char file = x.charAt(0);
        if (file < 'a' || file > 'i') {
            throw new IllegalArgumentException("x must be between a and i");
        }
        if (y < 0 || y > 9) {
            throw new IllegalArgumentException("y must be between 0 and 9");
        }
    }
}
