# Project Short Card

## Current Task
Finish Task 3: commit board initialization and helpers.

## Current State
- Task 2 code and tests are written and verified.
- Task 2 is committed in `cb503dc`.
- Outer project directory has been renamed to `chess-work`, removing the previous `!` path issue.
- First-pass package/file skeletons for the planned architecture are created; most behavior is still placeholder.
- User prefers ordinary Java classes over `record` declarations for readability; skeleton classes have been converted.
- Lombok dependency has been added; use `@Getter` to reduce boilerplate, not `@Data`. Use normal JavaBean accessors consistently.
- `Piece.isVisible()` is derived from whether `revealedType` is present; no separate internal visible boolean.
- Keep internal model validation lightweight; preserve coordinate/rule validation but avoid enterprise-style null checks.
- Task 3 code and tests are implemented and verified.

## Immediate Actions
1. Review Task 3 diff.
2. Commit `Board` and `BoardTest`.
3. Start Task 4: `FlipPool`.

## Completion Criteria
- `BoardTest` passes.
- Task 3 is committed before moving to flip pool.
