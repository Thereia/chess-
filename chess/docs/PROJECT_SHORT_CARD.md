# Project Short Card

## Current Task
Continue Task 8: connect WebSocket room flow and then add per-turn timeout scheduling.

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
- Task 6 `MoveExecutor` / `MoveRecord` adjustments are committed in `b4599f9`.
- `MoveExecutor.apply` executes already-validated moves only. `GameRoom` should call `RuleEngine` first, then call `MoveExecutor` only for accepted moves.
- `MoveRecord` has been simplified to operation plus reveal/capture facts: move number, color, from/to, flip result, captured piece, server time, and end reason.
- Keep only local game records required by the assignment; do not add extra history APIs or UI.
- Project docs were adjusted to remove history-playback feature wording while preserving local game-record requirements.
- Task 7 game-result / local-record flow is committed in `60407be`.
- Task 7 added room-level move handling, local `GameRecorder`, post-move finish checks, `resign`, and safe `timeout` settlement entrypoints.
- `GameRoom.timeout(expiredColor, expectedDeadline)` only settles when game status, current turn, and deadline still match, so later timer tasks can call it safely.
- `Ready` is a required protocol stage: after `matchSuccess`, both players must send `Ready` before `gameStart` and `PLAYING`.
- Focused verification passed with `mvn -q "-Dtest=MoveRecordTest,MoveExecutorTest,GameRecorderTest,GameRoomTest" test` through the temporary `subst X:` path workaround.
- Windows + current local `javac` still has real-path classpath issues; use temporary `subst X:` mapping when running tests.

## Immediate Actions
1. Implement `RoomManager.startMatch` / `roomForPlayer` and create a preparing room after matching the second player.
2. Implement required `Ready` handling so both players must send `Ready` before `gameStart` and `PLAYING`.
3. Implement `GameWebSocketHandler.handleTextMessage` for `startMatch`, `Ready`, `move`, `Resign`, and protocol errors while keeping business logic in room/domain classes.
4. After room flow is connected, add per-turn timeout scheduling that calls `GameRoom.timeout(expectedColor, expectedDeadline)`.

## Completion Criteria
- Two clients can match, both send `Ready`, receive `gameStart`, and then reach room-level `move`, `resign`, and timeout settlement through WebSocket.
- Timeout uses scheduled server-local timing, not client clocks.
- No history-playback API/UI work is introduced; only local game records remain in scope.
