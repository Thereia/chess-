# Project Short Card

## Current Task
Finish Task 6: review and commit `MoveExecutor`.

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
- Task 6 `MoveExecutor` code and tests are implemented and verified.

## Immediate Actions
1. Review Task 6 diff.
2. Commit `MoveExecutor` and `MoveExecutorTest`.
3. Start Task 7: game result checking / move record integration as planned.

## Completion Criteria
- `MoveExecutorTest` passes with existing core tests.
- Task 6 is committed before moving to result/record flow.
