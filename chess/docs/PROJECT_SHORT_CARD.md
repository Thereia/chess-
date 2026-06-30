# Project Short Card

## Current Task
Handoff for new session: review and commit current Task 6 changes, then start Task 7.

## Current State
- Task 2 code and tests are written and verified.
- Task 2 is committed in `cb503dc`.
- Outer project directory has been renamed to `chess-work`, removing the previous `!` path issue.
- First-pass package/file skeletons for the planned architecture are created; most behavior is still placeholder.
- User prefers ordinary Java classes over `record` declarations for readability; skeleton classes have been converted.
- Lombok dependency has been added; use `@Getter` to reduce boilerplate, not `@Data`. Use normal JavaBean accessors consistently.
- `Piece.isVisible()` is derived from whether `revealedType` is present; no separate internal visible boolean.
- Keep internal model validation lightweight; preserve coordinate/rule validation but avoid enterprise-style null checks.
- Task 3 `Board` code and tests are committed in `97ab08e`.
- Task 4 `FlipPool` code and tests are committed in `71d7b9f`.
- Task 5 `RuleEngine` code and tests are committed in `7a96bbc`.
- Task 6 `MoveExecutor` code and tests are implemented and verified, but not yet committed in this handoff.
- `MoveExecutor.apply` executes already-validated moves only. `GameRoom` should call `RuleEngine` first, then call `MoveExecutor` only for accepted moves.
- `MoveRecord` has been simplified to operation plus reveal/capture facts: move number, color, from/to, flip result, captured piece, server time, and end reason.
- Keep only local game records required by the assignment; do not add extra history APIs or UI.
- Project docs were adjusted to remove history-playback feature wording while preserving local game-record requirements.
- Current uncommitted work should include docs plus `MoveRecord`, `MoveExecutor`, `MoveExecutorTest`, and `MoveRecordTest`.
- Last focused verification passed with `mvn -q "-Dtest=PositionTest,PieceTest,BoardTest,FlipPoolTest,RuleEngineTest,MoveExecutorTest,MoveRecordTest" test` via the temporary `subst X:` path workaround.

## Immediate Actions
1. Run `git status --short` and review the uncommitted Task 6/doc diff.
2. Commit Task 6 and related doc/card updates if the diff looks right.
3. Start Task 7: game result / local record file flow, keeping the WebSocket handler thin.

## Completion Criteria
- Task 6 implementation and tests are committed.
- No history-playback API/UI work is introduced; only local game records remain in scope.
- Next session can continue from Task 7 without rereading the long card unless short card seems stale.
