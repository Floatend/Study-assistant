# Changelog

## 2026-09-07 - Public entry hierarchy and responsive reading (Stage A)

### Changed

- Reduced the owner-login link's visual weight while preserving its destination and legible brand color. Promoted the homepage project anchor to the primary action; kept the notes link secondary and all original content, images, palette and project records unchanged.
- Replaced the abrupt 1101px three-column reader with single-column, 1024px article/outline, and 1280px full-sidebar layouts. Archive results use separate column rules; intermediate-width readers retain the library drawer. Capped readable line length and gave table cells a minimum width inside their existing horizontal scroller.
- Added durable note-discovery regression coverage and a dedicated responsive-layout suite. Made the existing login color check wait for the hover transition to settle rather than inspect an interpolated color.

### Verification

- Production build and 10 unit tests pass. All 45 browser checks pass: 8 entry/layout, 13 note-discovery, 15 project and 9 lazy-UI checks. Fixtures are isolated from MySQL and the user's clipboard.
- At 1101px the article body grows from 501px to 760px. Breakpoint tests cover 320 through 1920px, portrait/landscape, long math/code/tables, drawer/category access, focus, history and reading recovery. A 720px CSS reflow check is not a claim of native browser 200% zoom testing.
- Existing homepage budgets pass: decoded JS 174,681B, CSS 27,822B, unchanged 162,060B mobile and 327,508B desktop hero payloads; observed CLS 0. Controlled local median LCP is 868ms mobile and 1220ms desktop, not a production guarantee. Existing build warnings remain.
- No backend, database, permission, FRP or VPS configuration changes. Docker/live deployment verification is not performed locally. See `docs/frontend-stage-a.md` for details and scoped PowerShell / sudo server commands. No commit, push or deployment performed.

## 2026-09-07 - Frontend improvement roadmap (planning only)

- Added `docs/frontend-improvement-plan.md` with page roles, design-system constraints, seven delivery batches, file boundaries, content dependencies and measurable acceptance criteria.
- Prioritized homepage navigation hierarchy and medium-width note reading for the first implementation batch. Documented safeguards for verified resume facts, publishing permissions, accessibility, performance and staged deployment.
- Documentation only: no application code, database, deployment configuration or running service changed. Implementation and browser acceptance remain pending; no build or application tests were run for this planning update.

## 2026-09-06 - Public page loading performance

### Changed

- Replaced full Element Plus registration/CSS with component-level imports using its standard Vue component resolver. Included service styles explicitly and preserved site overrides when route CSS arrives later.
- Extracted the existing private layout behind an async import and waited for initial router readiness before mounting. Public visits no longer temporarily load the owner layout, authentication utilities, forms or dialogs.
- Added reproducible WebP generation and responsive hero sources while preserving the original PNG files, visible composition, layout, typography and motion. The hero remains eager/high-priority; private workspace imagery is not loaded on public pages.
- Enabled static-response gzip and HTML revalidation in the frontend container's Nginx. Hashed assets keep an immutable cache, now for one year; API proxying remains unchanged.
- Fixed an existing unhandled rejection when cancelling or dismissing the notebook delete confirmation, found during development-server regression checks. Cancellation still sends no delete request.

### Measurements

- Compared with `82ab4e8`, total homepage decoded JS falls from 1,111,981 to 174,682 bytes; CSS from 378,294 to 26,989 bytes. Mobile hero payload falls from 2,841,840 to 162,060 bytes, desktop to 327,508 bytes.
- Controlled local production-preview tests (4 Mbps, 40ms latency, 3x CPU slowdown, three cold runs per viewport) recorded median LCP of 7,280 to 856ms on mobile and 7,276 to 1,200ms on desktop, with observed CLS 0. These are not deployed-site speed guarantees. Full method and compression distinctions are in `docs/frontend-performance.md`.

### Verification And Deployment

- Production build, 10 unit tests, 15 project browser checks, 13 existing note-discovery browser checks and 9 new lazy-UI/cache checks pass. Browser auth/note data is isolated; no live database writes were made.
- Added an executable performance budget test and browser coverage for deferred admin loading, validation, dialogs, editor/math rendering, loading directives, responsive imagery and repeat-visit resource reuse. Temporary screenshots and metrics stay outside the repository.
- Existing VueUse annotation and reader-bundle size warnings remain. Docker is unavailable locally, so live Nginx syntax/header and backend smoke checks remain part of deployment.
- Frontend-only rollout, including its embedded Nginx configuration. No SQL, backend, environment secret, FRP or VPS Nginx change. See `docs/frontend-performance.md` for reviewed staging, build, `nginx -t` and deployment commands. No automatic commit, push or deployment was performed.

## 2026-09-06 - Public project portfolio

### Added

- Public `/projects` overview and three independent project detail routes for the cloud-edge capture platform, LLM WeChat bot and 3D ceramic commerce platform. Details contain resume-supported background, responsibilities, implementation focus and conceptual workflow overviews, without invented metrics, screenshots, demo links or private contact details.
- Shared project stories layered on the existing timeline facts; homepage and timeline links now reach the detail pages. Detail pages support timeline preselection, previous/next projects, chapter navigation, back-to-top and an unknown-project recovery view.
- Topic-based related reading through the existing public search endpoint. Project content loads independently; note requests have empty/error/retry states, cancellation and stale-response guards. Returned titles render as escaped text.

### Changed

- Added a public project navigation entry and moved the owner login link onto the wordmark row on mobile. Navigation remains usable at 320px, project details keep the project entry active, and note/section headings clear the sticky header.
- Clarified that the website's early AI tools are historical experiments, not current public functionality. The eight previously verified awards are unchanged.
- Silent Axios requests now suppress business-error toasts as well as HTTP-error toasts. Existing non-silent errors and authentication handling remain unchanged.
- Retained the existing palette, Sakura homepage, glass navigation and Markdown/math rendering. New project content uses unframed sections, semantic color tokens and reduced-motion-aware reveals.

### Verification

- Frontend TypeScript/Vite production build and 10 unit tests passed, including 5 new portfolio tests.
- 15 Playwright project checks passed: direct links, data boundaries, homepage/timeline links, browser history, unknown slugs, note loading/retry/empty/business errors, escaped titles, stale responses, reader integration, keyboard navigation and normal/reduced motion. Screenshots cover 320/390/820/1440px with no page-level overflow.
- Re-ran all 13 existing note-discovery browser checks: search/pagination, categories, neighbors, resume/revision invalidation, async races, retry states, mobile drawers and heading offsets still pass after the shared navigation and error-handling changes.
- Browser note data is isolated test data, not live MySQL. No backend or database records were changed; real public-note integration still needs deployment smoke verification. Existing bundle-size and dependency-annotation build warnings remain.

### Deployment

- Frontend-only rebuild. No SQL migration, backend rebuild, secret, FRP or VPS Nginx change. The previous public note-discovery endpoint is reused.
- See `docs/project-portfolio.md` for scoped staging, tests, deployment-server commands and verification. Changes have not been automatically committed, pushed or deployed.

## 2026-09-06 - Note discovery and reading continuity

### Added

- A public archive/search view with server-side pagination (12 articles per page), result counts, page jumps, full-body search, literal keyword highlighting, and clean matched excerpts. Parent-category filtering now runs before pagination on the server, including existing slash, backslash, `>` and `::` category paths.
- Lightweight public search, related-article, and contextual neighbor endpoints. All queries require both published and official flags; drafts and ordinary published notes are excluded. Stable timestamp/id ordering keeps page boundaries and neighboring articles consistent.
- Opt-in local resume positions, keyed by article and update timestamp. At most 50 records are retained for 90 days; no article body is stored. Updated articles, completed reading, invalid storage, and blocked storage are handled without interrupting reading.
- Same-category recommendations, cross-page previous/next navigation, and explicit return-to-list actions that retain category, keyword, and page in browser history.

### Changed

- Kept article loading independent of search and related content. Failed auxiliary requests have retry states, and stale search/article responses cannot overwrite newer navigation.
- Replaced hand-written Markdown summary stripping with CommonMark-based plain-text extraction shared by search excerpts and existing note summaries. Frontmatter, math syntax, callout markers, raw HTML and Markdown delimiters stay out of summaries; frontend highlighting never uses raw HTML.
- Removed the duplicate desktop archive search box and kept the article title, category and return link below the fixed site navigation. Existing site colors, Sakura imagery, Markdown/KaTeX body renderer and glass controls remain unchanged.

### Verification

- Backend: 12 tests covering excerpt cleanup, a 137-public-note dataset, public/draft boundaries, body-only matches, literal SQL wildcard characters, category ancestry, neighbors, validation and controller response shape.
- Frontend: 5 unit tests covering safe highlighting, versioned resume, start/end cleanup, corrupt/blocked storage and bounded metadata.
- Browser: 13 Playwright checks covering archive pagination, search snippets, XSS-safe text, categories, history, cross-page neighbors, related notes, actual scroll/resume/revision invalidation, stale requests, retries, keyboard focus and responsive layouts at 320/390/820/1440px.
- Production frontend build and backend tests passed. Existing Vite bundle-size/dependency-annotation warnings remain.
- Backend tests use isolated H2 in MySQL mode; browser tests intercept only their own public API requests. No production MySQL data was accessed or changed. Live MySQL and deployed API smoke checks remain necessary after rollout.

### Deployment

- Rebuild both `goalbot-backend` and `goalbot-frontend`; no SQL migration, secret changes, FRP changes or VPS Nginx changes are required.
- See `docs/note-discovery.md` for API details, local checks and targeted deployment commands. The old public list endpoint and private notebook contracts remain compatible.

## 2026-09-06 - Mobile reading and personal introduction

### Changed

- Made public notes article-first on mobile and tablet, moving search, category navigation, and article selection into a drawer. Added persistent reading controls and a separate outline drawer while retaining desktop sidebars.
- Shared the library and outline components across layouts; kept Markdown, Obsidian callout, code, and KaTeX rendering unchanged.
- Replaced generic homepage slogans with the owner's education, development interests, and three resume-sourced projects. Project responsibilities expand on demand; the Sakura hero and existing palette remain, and article bodies stay on the notes route.
- Reused the timeline project data on the homepage instead of maintaining a second set of project facts. Kept public content free of private contact details and invented project metrics or screenshots.

### Fixed

- Article selection now creates browser history entries, preserving search and category context. Article changes scroll and focus the title after rendering; outline targets clear the fixed navigation. Cross-page navigation starts at the top, while browser page history can restore saved positions.
- Direct article URLs no longer depend on membership in the first 100 list results. Stale requests cannot replace a newer selection, and missing articles/list failures have visible retry states.
- Checked all eight awards against the owner's supplied screenshot: Huawei ICT is provincial third prize, and the China International College Students' Innovation Competition is campus second prize. Corrected that competition's Chinese title to match the source; the other six records were already consistent. No award dates were inferred.
- Home content remains visible without motion support and under reduced-motion preferences. Drawer Escape handling retains Element Plus focus restoration; article/heading selection focuses the reading destination after the drawer closes.

### Verification

- `npm.cmd run build` passed (Vue TypeScript check and Vite production build); existing dependency annotation and bundle-size warnings remain.
- 16 Playwright browser checks passed: article-first mobile layout, Markdown/KaTeX, drawer keyboard focus, heading offsets, article/history navigation, search/category/empty states, out-of-list deep links, error recovery, stale article requests and back-navigation during a slow category load, cross-page scroll, invalid links, normal/reduced motion, homepage project expansion, and all eight awards.
- Responsive checks and screenshots covered 320, 390, 820, 1100, and 1440px without page-level horizontal overflow.
- Local backend was not running. Reading tests intercepted public API responses only inside an isolated browser context; no database records were created or changed. Live backend integration remains to be checked after deployment.

### Deployment

- Frontend-only update; no SQL migration or backend rebuild is needed. After committing and pushing the updated frontend files and this changelog, run on the local deployment server:

```bash
cd /home/glg/goalbot/Study-assistant
git pull --ff-only origin main
docker compose build goalbot-frontend
docker compose up -d --no-deps --force-recreate goalbot-frontend
docker compose ps goalbot-frontend
```

## 2026-09-04 - Resume-based journey visualization

### Changed

- Replaced the fictional sequential journey list with resume-sourced education, project, competition, and honor data; private contact details remain excluded from the public site.
- Added a proportional education overview, a month-based 2026 project swimlane view, selectable timeline nodes with linked responsibility details, and an award map grouped by national, provincial, and campus scope.
- Kept awards without resume dates outside the chronological scale instead of inventing occurrence dates, and added contained horizontal exploration for narrow screens without causing page-level overflow.
- Added viewport-triggered motion: education and project tracks advance from left to right, while award groups, guide lines, and all eight award nodes unfold in a readable staggered sequence.
- Replaced the heavy pink timeline blocks with translucent paper bands, thin semantic track lines, start nodes, and restrained current-project endpoints; selected items now use a light tint instead of a solid fill.

### Verification

- `vue-tsc -b` and the Vite production build passed.
- Browser checks passed at 1440px and 390px widths; timeline selection updates the detail panel, all 4 project tracks and 8 award records render, and the page has no horizontal overflow.
- Browser animation checks confirmed the project reveal state, award delays from 220ms through 1.14s, completed end states, and reduced-motion fallbacks.

## 2026-09-03 - Liquid glass atmosphere

### Changed

- Replaced the page-corner color blobs with one shared `LiquidBackdrop` that renders three full-width green, sakura, and mist ribbons. The ribbons move independently behind every route and stop when reduced motion is requested.
- Rebuilt liquid glass as a reusable surface system with lower-opacity fills, stronger backdrop refraction, a restrained specular highlight, an inner lens outline, and layered depth shadows.
- Applied the glass system only to public navigation, timeline filters, note sidebars, the back-to-top control, login panel, workspace header, account control, and circular actions. Reading and editing surfaces remain opaque for clarity.

### Fixed

- Reduced the shared glass fill and highlight opacity so scenery and liquid color remain visible through controls instead of reading as an opaque white surface.
- Removed the hover-time highlight translation and expanded the highlight layer by one pixel, preventing the color wash from filling the whole control on hover or leaving a clipped gap after the pointer exits.
- Replaced solid public-navigation hover pills with a slim underline; the workspace entry keeps its glass boundary without changing fill geometry.

### Verification

- `vue-tsc -b` passed.
- `npm.cmd run build` passed with the existing dependency annotation and large-chunk warnings.
- Browser checks passed for `/`, `/notes`, and `/journey` at desktop, 390px, and 320px widths with no horizontal overflow; computed transforms confirmed that the background ribbons animate.

## 2026-08-15 - Server auto-update script

### Added

- Added `deploy/auto-update.sh`, a cron-friendly script that polls `origin/main`, pulls only when a new commit exists and the working tree is clean, then rebuilds the Docker containers and writes a log outside the repository.

### Verification

- Shell syntax check passed with `bash -n`.

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
