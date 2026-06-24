# Dialogue Agent Architecture

## Runtime Flow

```text
Feishu message
  -> AgentRuntime (session, idempotency, active work item)
  -> intent routing
  -> TaskDraftTurnParser (extract only this turn)
  -> TaskDraftReducer (merge with persistent state)
  -> constraint decision
       READY       -> execute TaskService
       NEEDS_INPUT -> ask for the smallest missing slot set
       CONFLICT    -> preserve valid slots and ask one targeted question
  -> transition audit
  -> Feishu reply
```

The parser is not allowed to write business data. The reducer is the only component that changes a draft. `TaskService` remains the only component that creates or updates final tasks.

## State Rules

1. A null slot from the current turn never erases a previously collected value.
2. Explicit current-turn values override stored inferred values.
3. `start_time + planned_minutes` derives `end_time`.
4. `start_time + end_time` derives `planned_minutes`.
5. An end time that is not after the start time is not silently accepted unless the expression explicitly identifies an overnight period.
6. A conflict keeps independently valid slots. For example, `现在开始，到中午十二点` at 19:35 keeps `start_time=19:35` but does not commit `end_time=12:00`.
7. Tool execution happens only when the reducer returns `READY`.

## Audit Trail

Every draft lifecycle change is represented as a transition:

```text
TASK_DRAFT_CREATED
TASK_DRAFT_REDUCED
TASK_DRAFT_COMPLETED
TASK_DRAFT_CANCELLED
```

Application logs provide a concise operational view:

```text
dialogue_transition userId=1 sessionId=20 draftId=10
type=TASK_DRAFT_REDUCED decision=READY
changedSlots=[end_time, planned_minutes, missing_slots]
```

MySQL keeps the full diagnostic payload in `conversation_transition_log`. Useful inspection query:

```sql
SELECT id, draft_id, transition_type, decision,
       state_before, semantic_frame, state_after,
       clarification_question, created_at
FROM conversation_transition_log
WHERE user_id = 1
ORDER BY id DESC
LIMIT 20;
```

## Dify Boundary

Dify Workflow may later return a structured `actions[]` plan, but it must not execute tools or own dialogue state. The backend validates every action, applies deterministic reducers, and executes business services transactionally. Dify Chat remains a response-polishing and free-conversation layer.

## Next Increment

The current reducer handles one active task draft reliably. The next planned increment replaces the singular draft assumption with a persistent work-item queue so one message such as `今天写高数卷子，新工科英语复习` can produce two task work items, collect shared or per-task times, and execute them independently without losing either action.
