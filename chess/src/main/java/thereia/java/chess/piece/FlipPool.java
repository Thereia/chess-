package thereia.java.chess.piece;

import java.util.List;
import java.util.Random;

public final class FlipPool {

    private final Random random;

    private FlipPool(Random random) {
        this.random = random;
    }

    public static FlipPool initial(Random random) {
        return new FlipPool(random);
    }

    public PieceType draw() {
        throw new UnsupportedOperationException("FlipPool.draw is implemented in Task 4");
    }

    public int remainingCount() {
        return 0;
    }

    public List<PieceType> remainingTypes() {
        return List.of();
    }

    Random random() {
        return random;
    }
}
