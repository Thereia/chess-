# Project Short Card

## Current Task
Start Task 3: board initialization and helpers.

## Current State
- Task 2 code and tests are written and verified.
- Task 2 is committed in `cb503dc`.
- Outer project directory has been renamed to `chess-work`, removing the previous `!` path issue.
- First-pass package/file skeletons for the planned architecture are created; most behavior is still placeholder.
- User prefers ordinary Java classes over `record` declarations for readability; skeleton classes have been converted.
- Lombok dependency has been added; use `@Getter` to reduce boilerplate, not `@Data`. Use normal JavaBean accessors consistently.
- `Piece.isVisible()` is derived from whether `revealedType` is present; no separate internal visible boolean.
- Keep internal model validation lightweight; preserve coordinate/rule validation but avoid enterprise-style null checks.

## Immediate Actions
1. Write failing tests for initial Jieqi board setup.
2. Implement `Board.initial()`, `Board.empty()`, lookup, put/remove/move, and path counting.
3. Run focused board tests and commit Task 3.

## Completion Criteria
- `BoardTest` passes.
- Task 3 is committed before moving to flip pool.
