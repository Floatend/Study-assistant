# GoalBot

GoalBot is a personal goal management AI assistant. The system stores goals, tasks, checkins, reviews, and notifications in MySQL. Dify and Feishu are external capabilities only: Dify generates advice from backend-prepared data, and Feishu sends reminders or receives future bot events.

## Project Layout

```text
.
|-- goalbot-backend   Spring Boot + MyBatis Plus + MySQL
`-- goalbot-frontend  Vue3 + Vite + TypeScript + Element Plus
```

## Cloud Deployment

Docker Compose deployment files are included at the repository root. See
[docs/cloud-deploy.md](docs/cloud-deploy.md) for the Ubuntu deployment guide.

For the recommended home-server architecture where GoalBot stays on a private LAN and an overseas VPS provides the public entry through FRP and Cloudflare, see
[docs/home-server-frp-cloudflare.md](docs/home-server-frp-cloudflare.md).

## Backend

Run MySQL first, then execute:

```sql
source goalbot-backend/sql/init.sql;
```

Environment variables:

```bash
MYSQL_URL=jdbc:mysql://localhost:3306/goalbot?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_password
GOALBOT_BOOTSTRAP_ADMIN_USERNAME=local_user
GOALBOT_BOOTSTRAP_ADMIN_PASSWORD=your_initial_admin_password
GOALBOT_AUTH_SESSION_DAYS=30
GOALBOT_REMINDER_ENABLED=true
DIFY_ENABLED=true
DIFY_API_URL=https://api.dify.ai/v1
DIFY_API_KEY=your_dify_api_key
DIFY_WORKFLOW_API_URL=
DIFY_WORKFLOW_API_KEY=
DIFY_PLANNER_API_URL=
DIFY_PLANNER_API_KEY=
DIFY_TIMEOUT_SECONDS=60
GOALBOT_AGENT_PLANNER_MODE=OFF
GOALBOT_AGENT_PLANNER_MIN_CONFIDENCE=0.72
GOALBOT_AGENT_PLANNER_MAX_ACTIONS=8
FEISHU_APP_ID=cli_xxxxxxxxx
FEISHU_APP_SECRET=your_feishu_app_secret
FEISHU_VERIFICATION_TOKEN=
FEISHU_ENCRYPT_KEY=
FEISHU_DEFAULT_CHAT_ID=oc_xxxxxxxxx
FEISHU_LONG_CONNECTION_ENABLED=false
```

Start backend:

```bash
cd goalbot-backend
mvn spring-boot:run
```

### Multi-user authentication

Existing databases must run the additive migration once before starting the new backend:

```sql
source goalbot-backend/sql/multi_user_auth.sql;
```

On Windows, run it from the repository root when `SOURCE` cannot resolve the relative path:

```powershell
$env:MYSQL_PWD='your_mysql_password'
Get-Content -Raw -Encoding UTF8 goalbot-backend/sql/multi_user_auth.sql |
  mysql --host=127.0.0.1 --port=3306 --user=root --default-character-set=utf8mb4
Remove-Item Env:MYSQL_PWD
```

The existing `local_user` becomes the first administrator. Its placeholder password is initialized exactly once from `GOALBOT_BOOTSTRAP_ADMIN_PASSWORD`. After the first successful startup, GoalBot stores only a PBKDF2 hash; keeping the environment variable does not reset an already initialized password.

Authentication endpoints:

```text
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout
PUT  /api/auth/password
```

Administrator endpoints:

```text
GET  /api/admin/users
POST /api/admin/users
PUT  /api/admin/users/{id}
PUT  /api/admin/users/{id}/password
```

All protected web APIs require `Authorization: Bearer <token>`. The old `X-User-Id` header is ignored and cannot switch the current user.

Main endpoints:

```text
GET    /api/dashboard
POST   /api/goals
GET    /api/goals
PUT    /api/goals/{id}
DELETE /api/goals/{id}
POST   /api/tasks
GET    /api/tasks
GET    /api/tasks/today
GET    /api/tasks/calendar
POST   /api/tasks/import/ics
PUT    /api/tasks/{id}/complete
POST   /api/checkins
GET    /api/checkins/recent
GET    /api/checkins/stats
POST   /api/ai/advice
POST   /api/ai/daily-review
POST   /api/ai/weekly-review
POST   /api/feishu/events
POST   /api/feishu/command/test
POST   /api/feishu/command/parse
POST   /api/dashboard/advice/refresh
POST   /api/reminders/daily-task
POST   /api/reminders/daily-review
POST   /api/reminders/weekly-review
GET    /api/notifications
```

AI schedule advice supports a selectable planning window:

```text
GET  /api/dashboard?adviceDays=1|2|3
POST /api/dashboard/advice/refresh?adviceDays=1|2|3
POST /api/ai/advice?days=1|2|3
```

`1` means today, `2` means today and tomorrow, and `3` means the next three days. The backend hashes the selected date range together with goals and tasks, so each range refreshes independently when its source schedule changes.

## Conversation Module

Feishu chat now writes assistant conversation context into MySQL. If your local database already exists, run the migration once:

```sql
source goalbot-backend/sql/conversation.sql;
```

The new tables are:

```text
conversation_session      active chat session, channel, state, topic, last intent
conversation_message      inbound/outbound message history for debugging and future memory
conversation_task_draft   unfinished task creation draft for multi-turn scheduling
conversation_transition_log deterministic before/frame/after audit trail for each draft change
```

The first persistent flow is natural-language task planning. For example, when the bot receives "I need to write the report tomorrow" without a time or duration, it stores a draft and asks a follow-up question. A comma-separated planning message can enqueue several drafts; GoalBot completes them in order and automatically introduces the next task. Final tasks are still created through the normal backend `TaskService`.

Existing databases created before the transition audit was added must run this additive migration once:

```sql
source goalbot-backend/sql/conversation_transition_log.sql;
```

The task-draft path now uses a deterministic semantic-frame reducer. It preserves slots collected in earlier turns, derives end time or duration when possible, and asks a targeted question when a time expression conflicts with the stored state. See [docs/dialogue-agent-architecture.md](docs/dialogue-agent-architecture.md) for the state rules and diagnostic query. Release-by-release behavior and migration notes are kept in [CHANGELOG.md](CHANGELOG.md).

While a draft is active, relative replies such as `接着高数` or `高数之后` are treated as schedule answers. GoalBot resolves the referenced task from MySQL and starts the current draft at that task's end time instead of creating or renaming a task.

GoalBot also includes an optional Dify Workflow planner that returns a validated `AgentPlan` with `actions[]`. Keep it `OFF` by default, evaluate it in `SHADOW`, and enable `PRIMARY` only after reviewing the audit log. Existing databases must run `goalbot-backend/sql/agent_plan_log.sql`. See [docs/dify-agent-planner.md](docs/dify-agent-planner.md) for the exact Dify nodes, prompt, JSON contract, environment variables, rollout steps, and acceptance checks.

## ICS Calendar Import

GoalBot can import timetable or calendar `.ics` files into MySQL-backed tasks. The frontend entry is on the `Calendar` page: choose an `.ics` file, set the import date range, preview the parsed events, then confirm import.

Backend endpoint:

```text
POST /api/tasks/import/ics
Content-Type: multipart/form-data

file: .ics file
dryRun: true | false, default true
startDate: yyyy-MM-dd, optional, default today minus 1 month
endDate: yyyy-MM-dd, optional, default today plus 6 months
skipExisting: true | false, default true
```

The importer supports common `VEVENT` fields: `SUMMARY`, `DESCRIPTION`, `LOCATION`, `UID`, `DTSTART`, `DTEND`, `DURATION`, and basic `RRULE` expansion for `DAILY`, `WEEKLY`, and `MONTHLY` events. It handles folded ICS lines, all-day events, local time, UTC `Z` time, `TZID`, `COUNT`, `UNTIL`, `INTERVAL`, and weekly `BYDAY`.

Duplicate protection is conservative: when `skipExisting=true`, an imported event is skipped if the same user already has a task with the same title, date, start time, and end time.

## Dify AI Test

Stage 3 uses Dify only as an AI generation service. GoalBot still stores all goals, tasks, checkins, reviews, and notification records in MySQL.

Recommended Dify app type for this stage:

```text
Chat App or Chatflow
```

Configure the backend runtime environment:

```bash
DIFY_ENABLED=true
DIFY_API_URL=https://api.dify.ai/v1
DIFY_API_KEY=your_dify_app_api_key
DIFY_TIMEOUT_SECONDS=60
```

If you deploy Dify locally, keep the backend code unchanged and switch only the URL:

```bash
DIFY_API_URL=http://localhost/v1
```

or:

```bash
DIFY_API_URL=http://your-server-ip/v1
```

Manual AI test endpoints:

```http
POST http://localhost:8080/api/ai/advice
POST http://localhost:8080/api/ai/daily-review
POST http://localhost:8080/api/ai/weekly-review
```

`/api/ai/advice` reads current goals, today's tasks, and recent 7-day checkins from MySQL, then sends a structured summary to Dify. The returned answer is saved to the `review` table with type `4`.

Dashboard also keeps today's advice current without blocking the whole page. `GET /api/dashboard` calculates a source hash from current goals and today's tasks, then returns the page data immediately with `aiAdviceStale`. If the advice is stale, the frontend calls `POST /api/dashboard/advice/refresh` in the AI card only. If the hash is unchanged, it reuses the saved advice and does not call Dify again. Temporary Dify fallback messages are saved without a source hash, so Dashboard can retry on later refreshes.

`/api/ai/daily-review` accepts an optional body:

```json
{
  "date": "2026-06-08"
}
```

`/api/ai/weekly-review` accepts an optional body:

```json
{
  "weekStart": "2026-06-01",
  "weekEnd": "2026-06-07"
}
```

If Dify is disabled, not configured, or temporarily unavailable, GoalBot saves a clear fallback message to `review` and keeps the core goal/task/checkin flows usable.

Natural-language command parsing now works in two layers:

```text
1. Strong backend rules: slash commands and very clear fixed commands.
2. Dify Workflow intent router: converts flexible language into structured intent JSON.
3. Local rule fallback: keeps basic commands usable when Workflow is not configured.
4. Dify Chat: free chat, AI advice, review generation, and later response polishing.
```

The workflow parser never writes business data directly. It only returns a structured intent frame. GoalBot validates that frame, decides whether to execute, ask a follow-up, ask for confirmation, or fall back to chat, then performs all reads/writes through backend services and MySQL.

If you want a separate Dify Workflow app for command parsing, configure:

```bash
DIFY_WORKFLOW_API_URL=
DIFY_WORKFLOW_API_KEY=
```

Recommended Jarvis-style routing:

```text
User message
  -> backend slash-command guard
  -> Dify Workflow intent JSON
  -> backend validation and DialogueManager
  -> TaskService / CheckinService / DifyService / other tools
  -> backend reply, or Dify Chat if it is ordinary conversation
```

The workflow receives inputs like:

```json
{
  "text": "离散作业写了 50 分钟，帮我打卡",
  "today": "2026-06-10",
  "supported_intents": ["TODAY_TASKS", "CREATE_TASK", "CHECKIN", "GOAL_STATUS", "ADVICE", "DAILY_REVIEW", "WEEKLY_REVIEW", "HELP", "UNKNOWN"],
  "today_tasks": [
    {
      "id": 1,
      "title": "离散作业",
      "goal_title": "完成课程作业",
      "planned_minutes": 50,
      "status": 0
    }
  ]
}
```

In the current backend, Dify Workflow inputs are sent as text-compatible values. Configure all Start variables as text inputs if you are unsure. `supported_intents`, `today_tasks`, `current_goals`, and `recent_messages` are JSON strings, not native arrays, because Dify `text-input` fields reject array/object values.

Recommended workflow output:

Use this extended intent frame shape:

```json
{
  "intent": "CREATE_TASK",
  "sentence_type": "COMMAND",
  "action_type": "WRITE",
  "task_title": "高数复习",
  "description": null,
  "plan_date": "2026-06-11",
  "start_time": "15:00",
  "end_time": "16:00",
  "planned_minutes": 60,
  "goal_id": null,
  "goal_keyword": "高数",
  "task_keyword": null,
  "actual_minutes": null,
  "missing_slots": [],
  "requires_confirmation": false,
  "clarifying_question": null,
  "assistant_reply": null,
  "confidence": 0.92
}
```

For query sentences such as `今日有任务吗`, return:

```json
{
  "intent": "TODAY_TASKS",
  "sentence_type": "QUESTION",
  "action_type": "READ",
  "confidence": 0.96,
  "missing_slots": [],
  "requires_confirmation": false
}
```

```json
{
  "intent": "CREATE_TASK",
  "task_title": "高数复习",
  "plan_date": "2026-06-11",
  "start_time": "15:00",
  "end_time": "16:00",
  "planned_minutes": 60,
  "goal_keyword": "高数",
  "confidence": 0.92
}
```

If `DIFY_WORKFLOW_API_URL` and `DIFY_WORKFLOW_API_KEY` are blank, but the normal Dify chat app is configured, GoalBot can also ask the chat app to return the same JSON shape. If Dify is unavailable, fixed commands and local natural-language rules still work.

## Feishu Reminder Test

Stage 2 now uses the Feishu self-built app bot, not a custom webhook bot. Configure app credentials and the default chat id in the backend runtime environment:

```bash
FEISHU_APP_ID=cli_xxxxxxxxx
FEISHU_APP_SECRET=your_feishu_app_secret
FEISHU_DEFAULT_CHAT_ID=oc_xxxxxxxxx
```

`FEISHU_DEFAULT_CHAT_ID` is the chat that receives scheduled daily task reminders, daily review reminders, and weekly review reminders.

Manual test endpoints:

```http
POST http://localhost:8080/api/reminders/daily-task
POST http://localhost:8080/api/reminders/daily-review
POST http://localhost:8080/api/reminders/weekly-review
```

Automatic reminders are controlled by:

```bash
GOALBOT_REMINDER_ENABLED=true
```

Set it to `false` when you want to keep manual tests available but disable scheduled sends.

## Feishu Command Test

Stage 4 uses backend-side command parsing and command execution logging. Before testing through the Feishu bot, you can test commands locally:

```http
POST http://localhost:8080/api/feishu/command/test
Content-Type: application/json
```

```json
{
  "text": "/今日"
}
```

Use the parse-only endpoint when you want to inspect intent recognition without writing checkins or calling Dify review/advice generation:

```http
POST http://localhost:8080/api/feishu/command/parse
Content-Type: application/json
```

```json
{
  "text": "离散作业写了 50 分钟，帮我打卡"
}
```

Example response:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "intent": "CHECKIN",
    "taskKeyword": "离散作业",
    "actualMinutes": 50,
    "confidence": 0.86,
    "source": "rule"
  }
}
```

Supported fixed commands:

```text
/今日
/打卡 任务名 50分钟
/进度
/建议
/复盘
/周报
```

Supported natural-language examples:

```text
今天有什么任务
明天下午 3 点安排高数复习 60 分钟
周五 9点到10点创建英语听力任务
离散作业写了 50 分钟，帮我打卡
高数学了 1.5 小时
今天该先做什么
目标现在怎么样
生成今天复盘
本周总结一下
```

`/打卡 任务名` matches today's tasks by title keyword. If exactly one task matches, GoalBot creates a checkin, marks the task completed, and updates the related goal status and task counts. The duration is optional: when omitted, GoalBot records the task's `planned_minutes`; an explicit duration such as `/打卡 任务名 50分钟` overrides it. If multiple tasks match, GoalBot asks for a more specific task name.

Natural-language checkin works the same way: `打卡物理` only needs a task keyword, while `物理学了 50 分钟，帮我打卡` may provide an actual duration. The backend matches today's task and writes the checkin; Dify only parses the command and does not store data.

Natural-language task creation creates real records in the `task` table. Common examples:

```text
明天下午 3 点安排高数复习 60 分钟
后天晚上 8点到9点创建英语听力任务
周五 9点到10点新增项目开发任务
```

The parser extracts `task_title`, `plan_date`, `start_time`, `end_time`, `planned_minutes`, and optional goal matching information. If no date is supplied but task creation is clear, GoalBot defaults the task date to today.

Command execution is recorded in `command_log`. The table also has a unique index on `feishu_message_id`, so redelivered Feishu events are ignored across backend instances that share the same MySQL database. Existing local databases need the new `command_log` table from `goalbot-backend/sql/init.sql`.

## Feishu Long Connection Bot

For local development, use Feishu's long connection event subscription. This avoids public HTTPS callback setup while you are still running GoalBot on your own machine.

Configure the Feishu self-built app:

```text
1. Enable App Capability > Bot.
2. Add permissions for receiving messages and sending messages as bot.
3. In Event Subscription, choose long connection.
4. Subscribe to message receive event, usually im.message.receive_v1.
5. Publish the app version and add the bot to a test chat.
```

Configure backend runtime environment:

```bash
FEISHU_APP_ID=cli_xxxxxxxxx
FEISHU_APP_SECRET=your_feishu_app_secret
FEISHU_LONG_CONNECTION_ENABLED=true
```

If the event subscription page gives you a verification token or encrypt key, configure them too:

```bash
FEISHU_VERIFICATION_TOKEN=your_verification_token
FEISHU_ENCRYPT_KEY=your_encrypt_key
```

Restart the Spring Boot backend. When the log shows that the Feishu long connection client is starting, send one of these messages to the bot:

```text
/今日
/打卡 任务名 50分钟
/进度
/建议
/复盘
/周报
今天有什么任务
明天下午 3 点安排高数复习 60 分钟
离散作业写了 50 分钟，帮我打卡
今天该先做什么
生成今天复盘
```

The bot also supports a lightweight conversational planning flow. If the task is clear but the schedule is incomplete, GoalBot keeps a 30-minute in-memory draft and asks for the missing time or duration:

```text
User: 今天要写离散作业
Bot: 可以，我先把「离散作业」记成任务草稿。你准备放在几点开始，预计多久？
User: 下午 3 点，60 分钟
Bot: 已创建任务...
```

Messages that are not recognized as explicit GoalBot commands fall back to Dify-powered Feishu chat. This is meant for natural conversation and planning guidance only; database writes still go through backend command execution.

The long connection handler receives text messages, calls `FeishuCommandService`, and replies through the Feishu OpenAPI. If `user.feishu_user_id` matches the sender's Feishu user id, open id, or union id, GoalBot uses that active local user. An unbound or disabled Feishu account cannot access another user's data; the bot returns the sender open id so an administrator can complete the binding in User Management.

When a bound user first sends the bot a private (`p2p`) message, GoalBot automatically saves that conversation's `chat_id` to the user's `assistant_settings.feishu_chat_id`. Existing manual settings are never overwritten, and group chats are not selected automatically. A user can still replace the push target from the Settings page when group delivery is intentional.

Duplicate reply guard:

```text
GoalBot keeps a 10-minute in-memory cache of Feishu message ids.
If Feishu redelivers the same event, the backend ignores the duplicate message id.
The long connection handler also ignores messages sent by app bots.
```

If one user message still receives multiple replies, confirm that only one backend process is running with `FEISHU_LONG_CONNECTION_ENABLED=true`. Two local Spring Boot instances connected to the same Feishu app will both receive and reply to the same message.

## Frontend

Install dependencies and start Vite:

```bash
cd goalbot-frontend
npm install
npm run dev
```

The Vite dev server proxies `/api` to `http://localhost:8080`.

AI advice and review content render Markdown through `markdown-it` and sanitize generated HTML through `DOMPurify`. The Dashboard is the daily workspace: it shows today's tasks, quick checkin, AI advice, and goal management. The old Goals and Checkin routes redirect back to Dashboard.

## Phase 1 Acceptance Path

1. Create a goal in the `Dashboard` goal management section.
2. Create today's task in `Tasks` and bind it to the goal.
3. Submit a quick checkin from the `Dashboard` task list.
4. Confirm the task becomes completed.
5. Confirm the goal task count is updated.
6. Confirm `Dashboard` and `Analytics` show today's data.

## Phase 2 Acceptance Path

1. Configure `FEISHU_APP_ID`, `FEISHU_APP_SECRET`, and `FEISHU_DEFAULT_CHAT_ID`.
2. Call `POST /api/reminders/daily-task`.
3. Confirm the Feishu app bot sends the task reminder to the default chat.
4. Call `POST /api/reminders/daily-review`.
5. Call `POST /api/reminders/weekly-review`.
6. Query `GET /api/notifications` and confirm sent or failed status is recorded.

## Phase 3 Acceptance Path

1. Configure `DIFY_API_KEY`.
2. Restart the backend.
3. Create at least one goal and one task.
4. Open `Dashboard`.
5. Confirm the right-side AI advice appears automatically.
6. Create, edit, check in, or delete one of today's tasks, or edit one of the current goals.
7. Refresh `Dashboard` or wait for the automatic refresh.
8. Confirm today's AI advice updates and is saved in `review` with type `4`.
9. Call `POST /api/ai/daily-review`.
10. Call `POST /api/ai/weekly-review`.

## Phase 4 Foundation Acceptance Path

1. Call `POST /api/feishu/command/test` with `/今日`.
2. Confirm it returns today's task list.
3. Call it with `/进度`.
4. Confirm it returns goal status and task counts.
5. Call it with `/打卡 任务名 50分钟`.
6. Confirm a checkin is created, the task is completed, and the related goal task count is updated.
7. Call it with `离散作业写了 50 分钟，帮我打卡`.
8. Confirm the natural-language message creates the same kind of checkin.
9. Call it with `今天该先做什么`.
10. Confirm it returns Dify-generated advice or a clear Dify fallback message.
11. Call `POST /api/feishu/command/parse` with `生成今天复盘`.
12. Confirm it returns `DAILY_REVIEW` without creating a review.
13. Call `POST /api/feishu/command/parse` with `明天下午 3 点安排高数复习 60 分钟`.
14. Confirm it returns `CREATE_TASK` with task title, date, time, and planned minutes.
15. Call `POST /api/feishu/command/test` with `明天下午 3 点安排高数复习 60 分钟`.
16. Confirm a task is created in the `task` table.
17. Call `POST /api/feishu/command/test` with `生成今天复盘`.
18. Confirm it creates or updates today's daily review.
19. Confirm `command_log` records the parsed intent, source, success flag, and reply.
20. Enable `FEISHU_LONG_CONNECTION_ENABLED=true`.
21. Send `/今日` to the Feishu bot.
22. Confirm the bot replies with today's task list.
