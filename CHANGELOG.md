# Changelog

This file records user-visible behavior changes, database migrations, and verification results. Add new entries at the top and keep each entry tied to the date it was implemented.

## 2026-06-24

### Added

- Added a deterministic task-draft pipeline: `TaskDraftTurnParser -> TaskDraftReducer -> tool execution`.
- Added typed per-turn semantic frames with slot source metadata for date, start time, end time, and duration.
- Added `conversation_transition_log` to persist `state_before`, `semantic_frame`, `state_after`, the reducer decision, and the clarification sent to the user.
- Added structured application logs named `dialogue_transition` with user, session, draft, transition type, decision, and changed slots.
- Added regression coverage for multi-turn time completion, Chinese time expressions, English duration units, and null-slot preservation.

### Changed

- Active task drafts now recognize replies such as `现在开始`, `到中午十二点`, `60min`, `2h`, `结束`, and `截止` as slot-completion turns.
- Task-draft tools no longer merge nullable values directly. Only explicitly extracted slots update the existing draft.
- A duration derives the end time from the stored start time. An explicit end time derives the duration.
- Task draft creation, reduction, completion, and cancellation now produce lifecycle audit entries.
- Checkin commands may omit actual duration. The backend uses the task's planned duration when no override is supplied.

### Fixed

- Fixed `现在开始，到中午十二点` losing its end-time phrase because draft continuation calls only carried raw text.
- Fixed a later `60min` reply causing the assistant to ask for start time again.
- Fixed ambiguous same-day end times being silently converted into invalid schedules. The assistant now keeps valid slots and asks one targeted clarification.
- Fixed empty fields from later turns overwriting start time or duration collected in earlier turns.

### Database

Existing databases must run:

```sql
SOURCE C:/absolute/path/to/goalbot-backend/sql/conversation_transition_log.sql;
```

Fresh databases receive the table through `goalbot-backend/sql/init.sql`. Docker volumes that already contain MySQL data do not rerun initialization scripts, so they still need the additive migration.

### Verification

- Backend compile succeeded with Java 17 target.
- Backend clean test: 9 passed, 0 failed.
- Frontend production build succeeded. Vite reported only the existing large-chunk optimization warning.
