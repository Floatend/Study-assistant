# Dialogue Agent Architecture

## Runtime Flow

```text
Feishu message
  -> AgentRuntime (session, idempotency, active work-item queue)
  -> optional Dify AgentPlanner (OFF / SHADOW / PRIMARY)
       -> AgentPlan validation and audit
       -> ordered allow-listed actions[]
  -> planner-selected tool or legacy intent fallback
  -> NaturalTaskListParser (zero, one, or many task titles)
  -> RelativeTaskTimeResolver (optional reference to another task's end time)
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
8. Relative phrases such as `接着高数` resolve against MySQL tasks on the draft date. The referenced task ID is recorded as the `start_time` slot source.
9. A missing or ambiguous task reference is a reducer conflict, never a new task title.
10. `CreateTaskTool` cannot overwrite an existing active draft. A misrouted create frame returns clarification instead of mutating the queue.

## Audit Trail

Every draft lifecycle change is represented as a transition:

```text
TASK_DRAFT_CREATED
TASK_DRAFT_QUEUED
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

Dify Workflow returns a structured `AgentPlan` with `TOOL`, `CHAT`, `CLARIFY`, or `UNKNOWN` mode and zero or more `actions[]`. It does not execute tools or own dialogue state. The backend supplies user-owned context, enforces an action limit and tool allow-list, rejects malformed plans, validates references, applies deterministic reducers, and executes business services through `ToolExecutor`.

Planner rollout has three modes:

```text
OFF     do not call the planner
SHADOW  record the plan, execute the legacy route
PRIMARY execute valid high-confidence plans, otherwise use the legacy route
```

Every attempted plan is written to `agent_plan_log`. The detailed Workflow contract and rollout procedure are in [dify-agent-planner.md](dify-agent-planner.md). Dify Chat remains the free-conversation and response-generation layer.

## Multi-task Queue

One natural-language message may create several `conversation_task_draft` rows in the same session. The oldest collecting row is the active work item; later rows remain queued. Completing or cancelling the active row advances the session to the next row and asks only for that task's missing schedule. Every row has an independent transition history.

The backend task-list rule is intentionally conservative. It bypasses Dify only when the message contains explicit planning language or every extracted item looks actionable. Ordinary comma-separated chat continues to free conversation.

## Relative Time References

`接着高数`, `高数之后`, and `等高数结束后` are schedule-slot answers when a task draft is active. GoalBot searches the current draft date for matching task titles and copies the unique match's end time into the draft start time. `接着上一个` selects the latest ending scheduled task on that date.

The resolver does not guess when several tasks match, when no task matches, or when the referenced task has no end time. It returns one clarification and leaves the active draft unchanged except for its transition audit entry.

## Next Increment

The next increment should add durable confirmation state for destructive or high-impact plans and an execution outbox for recoverable multi-action writes. The current Planner already supports ordered `actions[]`, structured multi-task creation, and relative task references; the persistent queue and deterministic reducer remain the authoritative execution layer.
