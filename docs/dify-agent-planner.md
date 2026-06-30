# Dify Agent Planner

## Purpose

The planner turns one user message plus GoalBot's current database context into a structured `AgentPlan`. It does not write MySQL, call Feishu, or claim that an operation succeeded. The Spring Boot backend validates the plan and executes allow-listed tools through the existing service layer.

```text
Feishu message
  -> Spring Boot assembles user-owned context
  -> Dify Workflow returns AgentPlan JSON
  -> backend validates mode, confidence, action count, tools, targets, and arguments
  -> ToolExecutor
  -> TaskService / GoalService / ReviewService
  -> MySQL
  -> reply
```

The old parser remains available as a fallback. Rollout is controlled by `OFF`, `SHADOW`, and `PRIMARY` modes.

## Create The Workflow

Create a Dify **Workflow** application, not a Chat App or Chatflow. Add these six Start variables and set every variable type to **Text Input**:

| Variable | Required | Meaning |
| --- | --- | --- |
| `user_text` | yes | current user message |
| `current_time` | yes | backend time with offset |
| `timezone` | yes | currently `Asia/Shanghai` |
| `context_json` | yes | active draft, queued drafts, candidate tasks, goals, and recent messages |
| `supported_tools` | yes | JSON string containing the backend tool allow-list |
| `planner_contract` | yes | authoritative output contract and planner constraints |

All six values are strings. Do not define `supported_tools` or `context_json` as an array/object input in Dify. This avoids the `text-input ... must be a string` request error.

Connect the nodes as follows:

```text
Start -> LLM -> End
```

Use a capable instruction-following model for the LLM node, set temperature around `0.1`, and insert all six Start variables into the prompt with Dify's variable picker. A suitable system prompt is:

```text
You are GoalBot's planning layer. Convert the current user turn into exactly one AgentPlan JSON object.

The backend, not you, owns state and executes tools. Never claim that data was created, updated, checked in, or deleted. Never invent task IDs or goal IDs. IDs may only come from context_json. Follow planner_contract exactly. Use only tools listed in supported_tools.

Resolve pronouns, omitted subjects, short answers, and relative scheduling expressions from active_draft, queued_drafts, candidate_tasks, and recent_messages. If one interpretation is not sufficiently supported, return CLARIFY with one focused question. For ordinary conversation return CHAT. Do not output Markdown, code fences, explanations, or chain-of-thought.

Current time: {{current_time}}
Timezone: {{timezone}}
User text: {{user_text}}
Context: {{context_json}}
Supported tools: {{supported_tools}}
Contract: {{planner_contract}}
```

Use the Dify variable picker rather than typing braces manually if your Dify version uses node-qualified variable names.

In the End node, expose one Text output named `plan_json` and bind it to the LLM node's complete text output. Publish the Workflow before copying its API key.

## Output Contract

The Workflow must return one JSON object:

```json
{
  "mode": "TOOL",
  "confidence": 0.96,
  "actions": [
    {
      "action_id": "a1",
      "tool": "update_task_draft",
      "target": {
        "type": "ACTIVE_DRAFT",
        "id": 102
      },
      "arguments": {
        "start_time_reference": {
          "relation": "AFTER",
          "boundary": "END",
          "task_id": 501,
          "task_query": "高数"
        }
      },
      "missing_slots": [],
      "requires_confirmation": false
    }
  ],
  "requires_confirmation": false,
  "clarifying_question": null,
  "assistant_reply": null
}
```

Mode meanings:

| Mode | Behavior |
| --- | --- |
| `TOOL` | validate and execute `actions` in order |
| `CHAT` | return `assistant_reply`, or call the existing free-chat tool when it is empty |
| `CLARIFY` | ask `clarifying_question` without writing business data |
| `UNKNOWN` | reject the plan and use the legacy backend route |

The backend accepts at most `GOALBOT_AGENT_PLANNER_MAX_ACTIONS` actions, rejects duplicated action IDs and unsupported tools, and falls back when confidence is below the configured threshold.

## Required Examples

### Continue after an existing task

Input: `接着高数`

When an active draft exists and candidate task `501` is the unique intended high-math task, return `update_task_draft` with `start_time_reference`. The backend verifies task ownership, date, and end time before updating the draft.

### Create several tasks

Input: `今天写高数卷子，新工科英语复习`

```json
{
  "mode": "TOOL",
  "confidence": 0.94,
  "actions": [
    {
      "action_id": "a1",
      "tool": "create_task",
      "target": {"type": "NONE", "id": null},
      "arguments": {
        "tasks": [
          {"title": "写高数卷子", "plan_date": "2026-06-24"},
          {"title": "新工科英语复习", "plan_date": "2026-06-24"}
        ]
      },
      "missing_slots": ["start_time", "duration"],
      "requires_confirmation": false
    }
  ],
  "requires_confirmation": false,
  "clarifying_question": null,
  "assistant_reply": null
}
```

GoalBot queues both drafts and asks for their schedules in order. A task object that already contains `start_time` plus `planned_minutes`, or `start_time` plus `end_time`, is ready for immediate creation.

### Continue the active draft with a duration

Input: `60min`

Return `update_task_draft`, target the ID from `active_draft`, and use:

```json
{
  "planned_minutes": 60
}
```

Do not turn `60min` into a new task title.

## Backend Configuration

Run the additive migration once on an existing database:

```sql
SOURCE C:/absolute/path/to/goalbot-backend/sql/agent_plan_log.sql;
```

Configure the planner Workflow separately from the existing advice/chat application:

```bash
DIFY_ENABLED=true
DIFY_PLANNER_API_URL=http://your-dify-host/v1
DIFY_PLANNER_API_KEY=app-your-workflow-api-key
GOALBOT_AGENT_PLANNER_MODE=SHADOW
GOALBOT_AGENT_PLANNER_MIN_CONFIDENCE=0.72
GOALBOT_AGENT_PLANNER_MAX_ACTIONS=8
```

`DIFY_PLANNER_API_URL` may be the `/v1` base URL or the full `/v1/workflows/run` URL. GoalBot normalizes either form.

## Safe Rollout

1. Start with `GOALBOT_AGENT_PLANNER_MODE=OFF` and apply the SQL migration.
2. Switch to `SHADOW`. GoalBot calls the Workflow and records the plan, but legacy routing still produces the user-visible reply.
3. Review at least 50 representative messages, especially active-draft continuation, multiple tasks, queries, cancellation, and free conversation.
4. Fix the Dify prompt until false writes and unnecessary clarifications are acceptably rare.
5. Switch to `PRIMARY`. Valid high-confidence plans execute; invalid, low-confidence, unavailable, or malformed plans automatically fall back to legacy routing.

Useful audit query:

```sql
SELECT id, user_id, run_mode, selected, plan_mode, confidence,
       primary_tool, error_message, plan_json, created_at
FROM agent_plan_log
ORDER BY id DESC
LIMIT 50;
```

Useful application log:

```text
agent_plan userId=1 sessionId=20 runMode=SHADOW selected=false
mode=TOOL confidence=0.94 tool=create_task source=dify-agent-plan
```

`SHADOW` currently waits for the Workflow response before continuing through the legacy route, so use it as a temporary evaluation mode rather than a permanent production setting. To roll back immediately, set `GOALBOT_AGENT_PLANNER_MODE=OFF` and restart the backend. No business-data migration is required for rollback.

## Acceptance Checks

1. `接着高数` produces `update_task_draft`, references an existing task ID, and uses that task's actual `end_time`.
2. `今天写高数卷子，新工科英语复习` preserves both titles in `conversation_task_draft`.
3. `60min` updates the active draft instead of creating a task named `60min`.
4. `今日有任务吗` produces a read tool, never `create_task`.
5. `随便聊聊生日怎么过` produces `CHAT` and performs no task write.
6. An invented task ID, unsupported tool, malformed JSON, timeout, or confidence below the threshold reaches the legacy fallback and performs no planner-selected write.
