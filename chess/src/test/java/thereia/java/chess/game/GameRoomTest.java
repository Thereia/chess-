package thereia.java.chess.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import thereia.java.chess.board.Board;
import thereia.java.chess.board.Position;
import thereia.java.chess.move.Move;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.FlipPool;
import thereia.java.chess.piece.Piece;
import thereia.java.chess.piece.PieceType;
import thereia.java.chess.protocol.GameOverMessage;
import thereia.java.chess.protocol.MoveResultMessage;
import thereia.java.chess.protocol.TimeoutMessage;
import thereia.java.chess.record.GameRecorder;
import thereia.java.chess.rule.MoveExecutor;
import thereia.java.chess.rule.RuleEngine;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class GameRoomTest {

    @TempDir
    Path dir;

    @Test
    void returnsMoveResultAndGameOverAfterCapturingKing() {
        GameRoom room = new GameRoom("room-1", new Player("red", ChessColor.RED), new Player("black", ChessColor.BLACK),
                state(Board.empty()
                                .put(Position.of("e", 0), Piece.visible("r-e0", ChessColor.RED, PieceType.KING))
                                .put(Position.of("e", 9), Piece.visible("b-e9", ChessColor.BLACK, PieceType.KING))
                                .put(Position.of("e", 1), Piece.visible("r-e1", ChessColor.RED, PieceType.ROOK)),
                        ChessColor.RED, 0, 0),
                new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));

        RoomMoveResult result = room.handleMove("red", move("e", 1, "e", 9), Instant.parse("2026-06-30T08:00:00Z"));

        MoveResultMessage moveResult = result.getMoveResult();
        GameOverMessage gameOver = result.getGameOver();

        assertThat(result.isSuccess()).isTrue();
        assertThat(moveResult).isNotNull();
        assertThat(moveResult.isSuccess()).isTrue();
        assertThat(moveResult.getToX()).isEqualTo("e");
        assertThat(moveResult.getToY()).isEqualTo(9);
        assertThat(gameOver).isNotNull();
        assertThat(gameOver.getWinnerColor()).isEqualTo("RED");
        assertThat(gameOver.getReason()).isEqualTo("checkmate");
        assertThat(room.getState().getStatus()).isEqualTo(GameStatus.ENDED);
        assertThat(room.getState().getWinnerColor()).isEqualTo("RED");
        assertThat(room.getState().getEndReason()).isEqualTo("checkmate");
    }

    @Test
    void declaresDrawWhenNoCaptureCountReachesEighty() {
        GameRoom room = new GameRoom("room-2", new Player("red", ChessColor.RED), new Player("black", ChessColor.BLACK),
                state(Board.empty()
                                .put(Position.of("e", 0), Piece.visible("r-e0", ChessColor.RED, PieceType.KING))
                                .put(Position.of("e", 9), Piece.visible("b-e9", ChessColor.BLACK, PieceType.KING))
                                .put(Position.of("e", 4), Piece.visible("r-e4", ChessColor.RED, PieceType.PAWN))
                                .put(Position.of("a", 0), Piece.visible("r-a0", ChessColor.RED, PieceType.ROOK)),
                        ChessColor.RED, 79, 12),
                new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));

        RoomMoveResult result = room.handleMove("red", move("a", 0, "a", 1), Instant.parse("2026-06-30T08:00:00Z"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMoveResult()).isNotNull();
        assertThat(result.getGameOver()).isNotNull();
        assertThat(result.getGameOver().getWinnerColor()).isNull();
        assertThat(result.getGameOver().getReason()).isEqualTo("drawNoCapture");
        assertThat(room.getState().getStatus()).isEqualTo(GameStatus.ENDED);
        assertThat(room.getState().getWinnerColor()).isNull();
        assertThat(room.getState().getEndReason()).isEqualTo("drawNoCapture");
    }

    @Test
    void resignEndsGameForOpponent() {
        GameRoom room = new GameRoom("room-3", new Player("red", ChessColor.RED), new Player("black", ChessColor.BLACK),
                state(Board.empty()
                                .put(Position.of("e", 0), Piece.visible("r-e0", ChessColor.RED, PieceType.KING))
                                .put(Position.of("e", 9), Piece.visible("b-e9", ChessColor.BLACK, PieceType.KING))
                                .put(Position.of("e", 4), Piece.visible("r-e4", ChessColor.RED, PieceType.PAWN)),
                        ChessColor.RED, 10, 5),
                new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));

        GameOverMessage gameOver = room.resign("red");

        assertThat(gameOver).isNotNull();
        assertThat(gameOver.getReason()).isEqualTo("resign");
        assertThat(gameOver.getWinnerColor()).isEqualTo("BLACK");
        assertThat(room.getState().getStatus()).isEqualTo(GameStatus.ENDED);
        assertThat(room.getState().getWinnerColor()).isEqualTo("BLACK");
        assertThat(room.getState().getEndReason()).isEqualTo("resign");
    }

    @Test
    void timeoutEndsGameForOpponentAndReturnsTimeoutMessage() {
        GameRoom room = new GameRoom("room-4", new Player("red", ChessColor.RED), new Player("black", ChessColor.BLACK),
                state(Board.empty()
                                .put(Position.of("e", 0), Piece.visible("r-e0", ChessColor.RED, PieceType.KING))
                                .put(Position.of("e", 9), Piece.visible("b-e9", ChessColor.BLACK, PieceType.KING))
                                .put(Position.of("e", 4), Piece.visible("r-e4", ChessColor.RED, PieceType.PAWN)),
                        ChessColor.RED, 7, 9),
                new RuleEngine(), new MoveExecutor(), new GameRecorder(dir));

        TimeoutMessage timeout = room.timeout(ChessColor.RED, room.getState().getTurnDeadlineAt());

        assertThat(timeout).isNotNull();
        assertThat(timeout.getColor()).isEqualTo("RED");
        assertThat(room.getState().getStatus()).isEqualTo(GameStatus.ENDED);
        assertThat(room.getState().getWinnerColor()).isEqualTo("BLACK");
        assertThat(room.getState().getEndReason()).isEqualTo("timeout");
    }

    private GameState state(Board board, ChessColor currentTurn, int noCapturePlyCount, int moveNumber) {
        Instant now = Instant.parse("2026-06-30T08:00:00Z");
        return new GameState(board, currentTurn, GameStatus.PLAYING, FlipPool.initial(new Random(1)),
                FlipPool.initial(new Random(2)), noCapturePlyCount, moveNumber, now, now.plusSeconds(60), null, null);
    }

    private Move move(String fromX, int fromY, String toX, int toY) {
        return new Move(Position.of(fromX, fromY), Position.of(toX, toY), false);
    }
}
