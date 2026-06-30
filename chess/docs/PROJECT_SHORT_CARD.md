# Project Short Card

## Current Task
Finish Task 5: review and commit `RuleEngine`.

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
- Task 5 `RuleEngine` code and tests are implemented and verified.

## Immediate Actions
1. Review Task 5 diff.
2. Commit `RuleEngine` and `RuleEngineTest`.
3. Start Task 6: `MoveExecutor` accepted-move application.

## Completion Criteria
- `RuleEngineTest` passes with existing core tests.
- Task 5 is committed before moving to move execution.
