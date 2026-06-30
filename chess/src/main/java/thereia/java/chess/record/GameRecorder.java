package thereia.java.chess.record;

import lombok.Getter;
import thereia.java.chess.move.MoveRecord;

import java.io.IOException;
import java.nio.file.Path;

@Getter
public final class GameRecorder {

    private final Path recordsDir;

    public GameRecorder(Path recordsDir) {
        this.recordsDir = recordsDir;
    }

    public void append(String roomId, MoveRecord record) throws IOException {
        throw new UnsupportedOperationException("GameRecorder.append is implemented in Task 7");
    }

}
