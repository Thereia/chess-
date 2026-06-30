package thereia.java.chess.game;

import lombok.Getter;
import thereia.java.chess.move.Move;
import thereia.java.chess.move.MoveRecord;
import thereia.java.chess.move.MoveValidationResult;
import thereia.java.chess.piece.ChessColor;
import thereia.java.chess.piece.PieceType;
import thereia.java.chess.protocol.MessageType;
import thereia.java.chess.protocol.GameOverMessage;
import thereia.java.chess.protocol.MoveResultMessage;
import thereia.java.chess.protocol.TimeoutMessage;
import thereia.java.chess.record.GameRecorder;
import thereia.java.chess.rule.MoveExecutor;
import thereia.java.chess.rule.RuleEngine;

import java.io.IOException;
import java.time.Instant;

@Getter
public final class GameRoom {

    private final String roomId;
    private final Player redPlayer;
    private final Player blackPlayer;
    private final RuleEngine ruleEngine;
    private final MoveExecutor moveExecutor;
    private final GameRecorder gameRecorder;
    private GameState state;

    public GameRoom(String roomId, Player redPlayer, Player blackPlayer, GameState state,
                    RuleEngine ruleEngine, MoveExecutor moveExecutor, GameRecorder gameRecorder) {
        this.roomId = roomId;
        this.redPlayer = redPlayer;
        this.blackPlayer = blackPlayer;
        this.state = state;
        this.ruleEngine = ruleEngine;
        this.moveExecutor = moveExecutor;
        this.gameRecorder = gameRecorder;
    }

    public RoomMoveResult handleMove(String playerId, Move move, Instant now) {
        Player player = playerFor(playerId);
        // 该死的防御性编程
        if (player == null) {
            return invalidMove(MoveValidationResult.illegal("player is not in room"), move);
        }
        if (state.getStatus() != GameStatus.PLAYING) {
            return invalidMove(MoveValidationResult.illegal("game is not playing"), move);
        }
        if (state.getCurrentTurn() != player.getColor()) {
            return invalidMove(new MoveValidationResult(false, 2002, "not this player's turn"), move);
        }

        MoveValidationResult validation = ruleEngine.validate(state.getBoard(), move, player.getColor());
        if (!validation.isValid()) {
            return invalidMove(validation, move);
        }

        MoveExecutor.MoveExecution execution = moveExecutor.apply(state, move, player.getColor(), now);
        GameState nextState = maybeFinishAfterMove(execution.getState(), execution);
        MoveRecord record = withEndReason(execution.getRecord(), nextState.getEndReason());
        this.state = nextState;
        appendRecord(record);

        MoveResultMessage moveResult = successMoveResult(move, execution, nextState);
        GameOverMessage gameOver = nextState.getStatus() == GameStatus.ENDED
                ? new GameOverMessage(MessageType.gameOver.name(), nextState.getWinnerColor(), nextState.getEndReason())
                : null;
        return new RoomMoveResult(true, moveResult, gameOver);
    }

    public GameOverMessage resign(String playerId) {
        Player player = playerFor(playerId);
        if (player == null) {
            throw new IllegalArgumentException("player is not in room");
        }
        if (state.getStatus() != GameStatus.PLAYING) {
            throw new IllegalStateException("game is not playing");
        }
        ChessColor winner = player.getColor().opponent();
        this.state = finishGame(state, colorName(winner), "resign");
        return new GameOverMessage(MessageType.gameOver.name(), state.getWinnerColor(), state.getEndReason());
    }

    public TimeoutMessage timeout(ChessColor expiredColor, Instant expectedDeadline) {
        if (state.getStatus() != GameStatus.PLAYING) {
            return null;
        }
        if (state.getCurrentTurn() != expiredColor) {
            return null;
        }
        if (!state.getTurnDeadlineAt().equals(expectedDeadline)) {
            return null;
        }

        ChessColor winner = expiredColor.opponent();
        this.state = finishGame(state, colorName(winner), "timeout");
        return new TimeoutMessage(MessageType.timeout.name(), colorName(expiredColor));
    }

    private Player playerFor(String playerId) {
        if (redPlayer.getPlayerId().equals(playerId)) {
            return redPlayer;
        }
        if (blackPlayer.getPlayerId().equals(playerId)) {
            return blackPlayer;
        }
        return null;
    }

    private RoomMoveResult invalidMove(MoveValidationResult validation, Move move) {
        MoveResultMessage moveResult = new MoveResultMessage(MessageType.moveResult.name(), false, validation.isValid(),
                validation.getCode(), validation.getMessage(), move.getFrom().getX(), move.getFrom().getY(),
                move.getTo().getX(), move.getTo().getY(), null, null, null);
        return new RoomMoveResult(false, moveResult, null);
    }

    private GameState maybeFinishAfterMove(GameState nextState, MoveExecutor.MoveExecution execution) {
        if (execution.getCapturedPiece() == PieceType.KING) {
            return finishGame(nextState, colorName(state.getCurrentTurn()), "checkmate");
        }
        if (nextState.getNoCapturePlyCount() >= 80) {
            return finishGame(nextState, null, "drawNoCapture");
        }
        return nextState;
    }

    private GameState finishGame(GameState baseState, String winnerColor, String endReason) {
        return new GameState(baseState.getBoard(), baseState.getCurrentTurn(), GameStatus.ENDED, baseState.getRedPool(),
                baseState.getBlackPool(), baseState.getNoCapturePlyCount(), baseState.getMoveNumber(),
                baseState.getTurnStartedAt(), baseState.getTurnDeadlineAt(), winnerColor, endReason);
    }

    private MoveRecord withEndReason(MoveRecord record, String endReason) {
        return new MoveRecord(record.getMoveNumber(), record.getColor(), record.getFrom(), record.getTo(),
                record.getFlipResult(), record.getCapturedPiece(), record.getServerTime(), endReason);
    }

    private void appendRecord(MoveRecord record) {
        try {
            gameRecorder.append(roomId, record);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to append move record", exception);
        }
    }

    private MoveResultMessage successMoveResult(Move move, MoveExecutor.MoveExecution execution, GameState nextState) {
        return new MoveResultMessage(MessageType.moveResult.name(), true, true, 0, "ok", move.getFrom().getX(),
                move.getFrom().getY(), move.getTo().getX(), move.getTo().getY(), pieceName(execution.getFlipResult()),
                pieceName(execution.getCapturedPiece()), nextState.getStatus() == GameStatus.ENDED
                        ? null
                        : colorName(nextState.getCurrentTurn()));
    }

    private String colorName(ChessColor color) {
        return color == null ? null : color.name();
    }

    private String pieceName(PieceType pieceType) {
        return pieceType == null ? null : pieceType.name();
    }
}
