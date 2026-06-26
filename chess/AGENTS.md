# Local Agent Instructions

## Project Memory Cards

This project uses two memory cards under `docs/`:

- `docs/PROJECT_SHORT_CARD.md`
- `docs/PROJECT_LONG_CARD.md`

Before starting work in a new session, read both cards:

1. Read `PROJECT_SHORT_CARD.md` first.
2. Read `PROJECT_LONG_CARD.md` next for full context.

Use the short card to decide the current task. Use the long card to understand background, decisions, risks, and unfinished work.

If the short card is empty, unclear, stale, or all listed tasks are complete, decide the next task from the long card and update `PROJECT_SHORT_CARD.md` before continuing.

## Card Responsibilities

`PROJECT_SHORT_CARD.md` records only the current task and immediate next actions. Keep it short.

`PROJECT_LONG_CARD.md` records the overall goal, project plan, important decisions, completed work, unfinished work, risks, and historical context.

When a task is completed:

1. Mark or move it into the completed section of `PROJECT_LONG_CARD.md`.
2. Refresh `PROJECT_SHORT_CARD.md` with the next current task.
3. Do not let the short card become a second long card.

## Project Direction

Build the simplest acceptable Jieqi assignment:

- One Spring Boot server.
- Two browser clients on the same computer.
- WebSocket JSON communication.
- Server-authoritative game state and rule validation.
- Local file game records.
- No database, Redis, login system, public deployment, spectator mode, chat, or AI in the first version.

Prefer simple implementation that satisfies the teacher's requirements over unnecessary backend infrastructure.
