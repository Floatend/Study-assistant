# Changelog

## 2026-08-15 - Note summary cleanup

### Fixed

- Replaced the note summary generator so headings, tables, LaTeX source, code fences, and Obsidian frontmatter no longer leak into the excerpt shown above a note. It now prefers a short tagline or the first readable paragraph and regenerates summaries when notes are read, so previously imported notes are fixed without a manual database update.

### Verification

- The modified backend service class compiled successfully with the project dependencies and Lombok annotation processing.

## 2026-08-15 - Frontend editorial refresh polish

- Fixed the compact public navigation so every route remains visible without horizontal scrolling on mobile.
- Removed default hero heading and paragraph margins so the homepage exposes the next section in the first viewport.
- Replaced duplicate public-note error toasts with one inline retry state.

This file records user-visible behavior changes, database migrations, and verification results. Add new entries at the top and keep each entry tied to the date it was implemented.

## 2026-08-15

### Changed

- Rebuilt the frontend around an Explore-first editorial system for the public site and an Operate-first system for the private notebook. The five-color palette now uses paper, warm surface, ink, sakura-leaf green, and sakura-pink semantic tokens.
- Replaced the former indigo gradients, equal-weight feature cards, oversized count blocks, and decorative shadows with flat content bands, ruled editorial rows, restrained motion, and functional liquid-glass navigation or tool surfaces.
- Updated the homepage, public notes reader, journey timeline, about page, login screen, shared navigation, Markdown renderer, category tree, note board, and private workspace to consume the shared color and spacing tokens.
- Limited interface typography to a Song-style display stack and the PingFang system body stack. Monospace remains only where source code content requires it.

### Verification

- Frontend type checking and production build passed after the visual-system migration.
- Desktop and mobile browser checks covered the public homepage, notes archive empty state, journey filters, about page, login page, and direct route loading without horizontal overflow.

## 2026-08-13

### Added

- Added a public `/journey` resume timeline page inspired by editorial, personal "making things" portfolio sites. It combines projects, learning paths, courses, and certificates into one filterable timeline with category counts and a now badge.
- Added a dedicated `src/data/timeline.ts` data file so the owner can add, remove, or reorder timeline entries without touching page markup.
- Added the timeline to the public site navigation and to the site changelog.

### Verification

- Frontend type checking and production build passed after adding the timeline page and route.
- Browser verification confirmed the desktop and mobile timeline render, filters update counts, and the empty certificate state is shown until real certificate entries are added.

## 2026-08-01

### Added

- Added a visual board view to the owner notebook with category boxes: drag a note tile into any category box to re-categorize it in one gesture, create empty category boxes ahead of time (persisted locally), and delete boxes that are still empty.
- Added a per-note tile menu on the board with edit, move-to-category (pick an existing category or type a new one), publish/unpublish, and delete actions; the board and the classic list view are switchable from the notebook toolbar.
- Added shared KaTeX math rendering for public notes, the administrator reader, and the editor preview. Notes now support inline `$...$` and `\(...\)` formulas plus display `$$...$$` and `\[...\]` formulas without interpreting formulas inside fenced code blocks.
- Added the generated project-local `linge-sakura-hero.png` asset and replaced the public homepage hero image with a cherry-blossom campus scene that keeps clear left-side title space.

### Changed

- Unified the public homepage, about, notes, login, public header, and admin shell onto one consistent editorial-blue design-token system: 20px rounded card surfaces with soft gradient tints, a deep-navy gradient hero and dark quote band, pill navigation and call-to-action buttons, and a card-based notes library with a raised article reader and sticky outline.
- Replaced the leftover warm-theme override blocks that had been stacked on top of the blue palette with single coherent styles per component, and removed the resulting dead CSS.
- Softened the homepage sakura petals (white-to-blush gradient at lower opacity) over the new navy hero so they read as a quiet accent instead of competing with the headline.
- Added a one-time, low-key sakura-petal entrance to the homepage hero while retaining pointer depth and respecting `prefers-reduced-motion`; it does not run as a continuous particle effect.
- Improved formula layout for long display equations with horizontal scrolling rather than clipping, and kept inline formula spacing aligned with the reading text.

### Verification

- Frontend type checking and production build passed after adding KaTeX. The build emitted only the existing dependency annotation and large-chunk warnings.
- Browser verification confirmed that the public homepage loads the Sakura asset, displays the hero copy cleanly, renders eight entrance petals, and has no desktop horizontal overflow.

## 2026-07-31

### Changed

- Restored the administrator notebook's missing workspace styles and reorganized it into a clear category-and-note library, focused article reader, and optional heading outline.
- Reworked the note editor into a viewport-bounded dialog with independently scrollable source and preview panes; mobile editing now opens in single-column write mode by default.
- Improved shared Markdown rendering with fenced-code-aware heading extraction, YAML highlighting, responsive tables and images, task-list checkboxes, horizontal rules, and accessible Obsidian callout folding.
- Split Obsidian callout titles from their body content so title colors, body text, and collapsed state render independently and consistently.
- Removed the former GoalBot assistant workspace, including task, goal, calendar, check-in, review, analytics, settings, user-management, Dify, Feishu, reminder, agent-planner, and conversation modules.
- Reduced the product to a public personal homepage, a public official-note archive, an about page, owner login, and an administrator-only Markdown notebook.
- Reduced the Spring Boot API to owner authentication plus private and public note endpoints; non-administrator accounts can no longer sign in to the private site.
- Removed ECharts and FullCalendar from the frontend bundle and removed the Lark SDK and assistant-only backend dependencies.
- Renamed user-facing branding and runtime settings to `linge.xin` and `SITE_*`. Existing Compose service names, Java package names, the `goalbot` database, and the `goalbot_mysql_data` volume remain unchanged for deployment compatibility.
- Replaced the assistant-oriented README and deployment documentation with site-only instructions and removed the obsolete Dify planner and dialogue-agent documents.
- Improved the login layout so its main headline wraps cleanly at desktop and mobile widths.
- Removed the public notes page intro sentence and replaced the flat category list with an expandable animated category tree. Categories support `/`, `>`, `::`, and `\\` path separators; selecting a parent category includes all descendant notes.
- Moved the category tree into a shared component used by both the public notes reader and the administrator notebook, with a visible empty state and category-path input guidance.
- Kept Obsidian callout colors, icons, borders, backgrounds, and fold behavior while hiding only the `NOTE`, `WARNING`, `TIP`, and similar type labels.
- Upgraded Obsidian note callouts from generic blockquotes to Obsidian-compatible `data-callout` markup with a title row, type label, color band, and light background.
- Replaced the placeholder Callout marker with type-specific Element Plus icon geometry: pencil for notes, lightning for tips/failures, and warning for caution states.
- Added `highlight.js` syntax highlighting for common note code languages, with language labels, a copy action, and an Obsidian-inspired light code surface.
- Preserved `[!NOTE]+` and `[!NOTE]-` fold state in rendered notes, while keeping fenced code examples unchanged.
- Tuned the `NOTE`, `TIP`, `WARNING`, and `DANGER` callout colors from the Things theme's blue and green palette.

### Database

- Replaced the initial schema with a repeatable, non-destructive definition for `user`, `auth_session`, and `note`.
- Existing assistant tables are not dropped automatically. Back up the database before archiving or removing them manually.
- Existing deployments must run `goalbot-backend/sql/init.sql` and `goalbot-backend/sql/note_knowledge_base.sql` once to create any missing note table, fields, or indexes.

### Verification

- Frontend type checking and production build passed after the notebook workspace and Markdown renderer repairs.
- Desktop browser checks passed for the three-column reader, tables, task items, code blocks, and interactive callout folding; the editor remains inside a 720px-high viewport.
- Mobile browser checks passed at 390 x 844 with no horizontal overflow, a compact library flow, and single-column editing by default.
- Backend `mvn clean test` passed after compiling the reduced 34-source Spring Boot application.
- Public-note APIs returned `code: 0` after the repeatable schema created the previously missing local `note` table.
- The consolidated note migration executed successfully twice against the same MySQL database, confirming repeatable column/index checks.
- The private note API returned the expected `401` response without an administrator token.
- Desktop and mobile browser checks passed for the public homepage, notes archive, owner login, and protected notebook redirect.
- Frontend type checking and production build passed after the category tree changes.
- Local `/notes` route returned HTTP 200 and no longer renders the removed intro sentence.
- Frontend production build passed after the callout DOM and style changes.
- Frontend production build passed after the icon and code-block rendering changes.
- Local Vite routes returned HTTP 200 for `/`, `/notes`, and `/about`.

## 2026-07-16

### Changed

- Added a reusable back-to-top control to public notes and the administrator notebook reader.
- Added shared Obsidian callout normalization for `> [!NOTE]`, `[!NOTE]`, and `!note` forms, while leaving fenced code examples unchanged.
- Rebalanced the public-site color system into a calmer intelligent-blue palette with pale blue, mint, and peach content bands; retained the existing motion and public/private workspace split.
- Brightened the public-site palette around coral, turquoise, yellow, and deep teal, and removed the public pages' serif English display fonts so they consistently inherit the PingFang system stack.
- Added lightweight public-site interaction: pointer-responsive hero depth, hover-responsive direction bands, a smooth-scroll exploration control, and a live reading-progress / active-heading state on public knowledge-base articles.
- Rebuilt the public `linge.xin` experience around an editorial, negative-space narrative: the homepage introduces the site and its direction without loading article content; `/about` now follows the same public visual language.
- Reworked `/notes` into a public learning archive with search, category filters, an article index, Markdown heading outline, and previous/next article navigation.
- Moved note authoring out of Dashboard into the administrator-only `/notebook` writing desk. It supports Markdown import, category management, draft/organized/public states, split editor-preview mode, and keyboard save.
- Restricted every `/api/notes` management endpoint to administrator accounts. Ordinary GoalBot users no longer have a notebook entry point or private-note API access.
- Kept public exposure explicit: `/api/public/notes` only returns notes that are both `is_published = 1` and `is_official = 1`.

### Added

- Added note categories, category indexes, and `GET /api/notes/categories` plus `GET /api/public/notes/categories`.
- Added shared Markdown heading IDs so the reader outline always targets the rendered article heading.
- Added the reusable `$monaka-editorial-web` Skill under `skills/monaka-editorial-web` and installed it in the local Codex skill directory.

### Database

Existing databases must run the repeatable migration once:

```sql
SOURCE /absolute/path/to/goalbot-backend/sql/note_knowledge_base.sql;
```

It adds `note.category` and the category lookup indexes without changing or deleting existing note data.

### Verification

- Backend Maven test reports: 29 tests, 0 failures, 0 errors.
- Frontend `npm run build` passed. Vite reported the existing large-chunk warning only.
- Local Vite routes returned HTTP 200 for `/`, `/notes`, `/about`, and `/notebook`.

## 2026-07-14

### Changed

- Reframed `/` as the personal promotional homepage for `linge.xin`; it no longer loads or renders note content directly.
- Added public subpages: `/notes` for official notes and `/about` for the personal site introduction.
- Reworked the public navigation so personal-site content and the private GoalBot workspace are intentionally separate.
- Replaced generic public-note visibility with an explicit official-site publishing workflow. A note must be marked as both official and published before it can reach `/api/public/notes`.
- Restricted official-site publishing to administrator accounts. Ordinary users can continue to keep and edit their own private notes, but cannot expose them through the public site.

### Added

- Added a generated, project-local personal workspace hero image and subtle motion that respects `prefers-reduced-motion`.
- Added `note.is_official` and the `goalbot-backend/sql/note_official.sql` migration for existing databases.

### Database

Existing databases should run the repeatable compatibility migration once:

```sql
SOURCE /absolute/path/to/goalbot-backend/sql/note_official.sql;
```

It safely adds any missing public-note columns and indexes, including the earlier `is_published` migration.

## 2026-07-13

### Changed

- Made `/` the public Markdown blog homepage. The GoalBot task, calendar, Feishu, and AI workspace now remains behind `/login` and `/dashboard`.
- Removed the decorative `G` square mark from the GoalBot navigation and login page.
- Standardized the web UI font stack around PingFang SC, with Microsoft YaHei and system fallbacks for environments where PingFang is unavailable.
- Added a clear “发布到公开博客 / 撤下公开博客” control to the personal notebook panel.

### Added

- Added read-only public endpoints: `GET /api/public/notes` and `GET /api/public/notes/{id}`.
- Added `note.is_published` so private working notes are never exposed unless their owner explicitly publishes them. New and uploaded notes are private by default.
- Added `goalbot-backend/sql/note_publication.sql` for existing databases.

### Database

Existing databases must run this additive migration once:

```sql
SOURCE /absolute/path/to/goalbot-backend/sql/note_publication.sql;
```

Fresh databases receive the new field through `goalbot-backend/sql/init.sql` and `goalbot-backend/sql/note.sql`.

## 2026-07-09

### Added

- Added a personal Blog/Notebook panel to the Dashboard so uploaded notes sit in the same workspace as tasks, goals, and AI advice.
- Added Markdown note upload support for `.md`, `.markdown`, and `.txt` files.
- Added manual note creation, note editing, note deletion, keyword search, note stats, and note detail preview.
- Added a Things-inspired note reader style with a clean paper surface, blue section headings, pink emphasis, green block quotes, and light code blocks.
- Added backend `/api/notes` CRUD and `/api/notes/upload` multipart endpoints.
- Added the `note` table and MyBatis Plus note service layer.

### Database

Existing databases must run:

```sql
SOURCE C:/absolute/path/to/goalbot-backend/sql/note.sql;
```

On Linux servers, use the deployed path, for example:

```sql
SOURCE /home/glg/goalbot/Study-assistant/goalbot-backend/sql/note.sql;
```

Fresh databases receive the table through `goalbot-backend/sql/init.sql`.

### Verification

- Backend test suite passed: 29 tests, 0 failures.
- Frontend production build passed. Vite reported the existing large-chunk warning.

## 2026-06-24

### Added

- Added an optional Dify Workflow `AgentPlanner` that returns structured `AgentPlan` objects with ordered `actions[]`.
- Added `OFF`, `SHADOW`, and `PRIMARY` planner rollout modes with confidence and action-count limits.
- Added planner context assembly for active and queued drafts, nearby tasks, goals, and recent messages.
- Added an allow-listed `AgentPlanExecutor`; Dify still cannot write MySQL or call business services directly.
- Added backend validation for Planner target types (`NONE`, `ACTIVE_DRAFT`, `TASK`, and `GOAL`).
- Added structured `start_time_reference` support so a plan can express “after task 501 ends” without inventing a clock time.
- Added native `tasks[]` execution in `CreateTaskTool`, allowing one plan to preserve several task titles without punctuation-based parsing.
- Added `agent_plan_log` and structured `agent_plan` application logs for shadow evaluation and production diagnosis.
- Added [docs/dify-agent-planner.md](docs/dify-agent-planner.md) with the complete Workflow contract and rollout procedure.
- Added a deterministic task-draft pipeline: `TaskDraftTurnParser -> TaskDraftReducer -> tool execution`.
- Added a persistent multi-task draft queue. A message such as `今天写高数卷子，新工科英语复习` now keeps both tasks and collects their schedules in order.
- Added a conservative backend task-list rule so explicit multi-task planning does not depend on Dify returning a singular `taskTitle`.
- Added deterministic relative task-time references such as `接着高数`, `高数之后`, `等高数结束后`, and `接着上一个`.
- Added typed per-turn semantic frames with slot source metadata for date, start time, end time, and duration.
- Added `conversation_transition_log` to persist `state_before`, `semantic_frame`, `state_after`, the reducer decision, and the clarification sent to the user.
- Added structured application logs named `dialogue_transition` with user, session, draft, transition type, decision, and changed slots.
- Added regression coverage for multi-turn time completion, Chinese time expressions, English duration units, and null-slot preservation.

### Changed

- Planner failures, malformed JSON, unsupported tools, low confidence, and unconfigured Workflow now fall back to the existing backend route.
- Dify chat, the existing intent Workflow, and the new planner Workflow use independent API keys and URLs.
- Active task drafts now recognize replies such as `现在开始`, `到中午十二点`, `60min`, `2h`, `结束`, and `截止` as slot-completion turns.
- Completing or cancelling the current queued draft now automatically introduces the next task instead of returning the conversation to `IDLE`.
- A relative time reference queries tasks on the current draft date and uses the matched task's `end_time` as the current draft's `start_time`.
- Missing, ambiguous, or unscheduled referenced tasks now produce a targeted clarification without changing the queued task title.
- Queue expiry is refreshed whenever the user updates the current task, preventing later queued items from expiring while the first item is being clarified.
- Task-draft tools no longer merge nullable values directly. Only explicitly extracted slots update the existing draft.
- A duration derives the end time from the stored start time. An explicit end time derives the duration.
- Task draft creation, reduction, completion, and cancellation now produce lifecycle audit entries.
- Checkin commands may omit actual duration. The backend uses the task's planned duration when no override is supplied.

### Fixed

- Fixed `现在开始，到中午十二点` losing its end-time phrase because draft continuation calls only carried raw text.
- Fixed a later `60min` reply causing the assistant to ask for start time again.
- Fixed ambiguous same-day end times being silently converted into invalid schedules. The assistant now keeps valid slots and asks one targeted clarification.
- Fixed empty fields from later turns overwriting start time or duration collected in earlier turns.
- Fixed comma-separated natural task lists reaching `CreateTaskTool` with `taskTitle=null` and returning “还缺任务名”.
- Prevented ordinary comma-separated chat from being treated as a task list unless planning language or actionable task phrases are present.
- Fixed `接着高数` being reclassified as a new `CREATE_TASK` intent and overwriting the queued `新工科英语复习` draft.
- Added a tool-layer invariant that a new singular `CREATE_TASK` frame cannot overwrite an existing active draft, even when an upstream classifier makes the wrong routing decision.

### Database

Existing databases must run:

```sql
SOURCE C:/absolute/path/to/goalbot-backend/sql/conversation_transition_log.sql;
SOURCE C:/absolute/path/to/goalbot-backend/sql/agent_plan_log.sql;
```

Fresh databases receive the table through `goalbot-backend/sql/init.sql`. Docker volumes that already contain MySQL data do not rerun initialization scripts, so they still need the additive migration.

### Verification

- Backend compile succeeded with Java 17 target.
- Backend clean test: 29 passed, 0 failed. Coverage includes Planner parsing and fallback, tool and target allow-list rejection, primary plan execution, structured multi-task arrays, structured and text-based relative task references, task-list routing, queue continuation, temporal reducer, draft overwrite protection, and checkin regressions.
- Frontend production build succeeded. Vite reported only the existing large-chunk optimization warning.
