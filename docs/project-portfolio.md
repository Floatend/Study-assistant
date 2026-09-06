# Project Portfolio

## Scope

This phase adds a public Explore experience, not a project administration system.

- `/projects`: three resume-sourced representative projects, with roles, dates, technical directions and compact workflow overviews.
- `/projects/cloud-edge-capture`: cloud/edge/device platform.
- `/projects/wechat-llm-agent`: LLM WeChat bot.
- `/projects/ceramic-commerce`: 3D ceramic commerce platform.
- Unknown slugs display a recovery view with a link to the overview.
- Homepage project entries and timeline inspectors link to details. Detail links to `/journey?project=<id>` select that project in the timeline. Previous/next links retain normal browser history.

No schema, credentials, backend endpoints, FRP configuration or VPS Nginx configuration change is needed. The public note search endpoint from the previous note-discovery phase must already be deployed for related reading to work.

## Content Maintenance

- `goalbot-frontend/src/data/timeline.ts` remains the source for titles, dates, roles, descriptions, tags and actual work. Previously corrected awards are unchanged.
- `goalbot-frontend/src/data/projects.ts` adds background, implementation focus, conceptual workflow steps and reading topics. It does not introduce a second copy of timeline facts.
- Conceptual workflows summarize resume-supported collaboration or business steps. They are not claimed as deployed architecture diagrams or product screenshots.
- No repository URLs, demo links, screenshots, performance metrics, private contact details or additional project achievements have been invented. Add genuine supporting material only when available and suitable for public release.
- The personal website stays in the timeline, outside this first three-project portfolio. Its description now distinguishes removed early AI experiments from the current public notes/portfolio site.

## Public Notes

`ProjectNotes.vue` requests `GET /api/public/notes/search` with `keyword`, `page=1` and `pageSize=3`. Topics are explicit selectors; these are related learning notes, not claimed project reports. The existing server is responsible for enforcing published + official visibility.

The project text and navigation do not wait for notes. Empty states link to the topic search; request failures have an inline retry. Topic changes and unmount cancel requests, and stale responses cannot overwrite a newer result. API titles use escaped Vue text, not raw HTML. Silent requests now suppress toast errors for both HTTP and business-level failures; normal request errors and authentication handling are unchanged.

## Verification

From `goalbot-frontend`:

```bash
npm ci
npm run test:unit
npm run build
```

The browser suite uses Playwright with an isolated context that intercepts public note APIs only. It does not create, update or delete database records. It needs a running frontend (default `http://127.0.0.1:5173`). Run it with an available Playwright runtime:

```bash
export PLAYWRIGHT_MODULE=/absolute/path/to/node_modules/playwright
# Optional when using an installed Chrome instead of Playwright's Chromium:
export CHROME_PATH=/absolute/path/to/chrome
node tests/projects.browser.cjs
```

Optional environment variables: `QA_BASE_URL`, `QA_OUTPUT_DIR`. Screenshots otherwise go to the system temp directory under `linge-projects-qa`.

Coverage: 5 portfolio unit tests (10 total with existing reading tests) and 15 browser checks for direct routes, shared content, public/private navigation, unknown slugs, timeline links, neighbors/history, independently loaded notes, empty/error/retry states, escaped titles, stale requests, Markdown/KaTeX reader integration, keyboard targets, reduced motion and 320/390/820/1440px layouts. Production build warnings about existing dependency annotations and bundle sizes remain.

Live public-note API behavior and the deployed version must also be smoke-tested after rollout; isolated fixtures are not a substitute for that check.

The 13 existing note-discovery browser checks were also rerun successfully after changing shared navigation and silent-error handling.

## Publish The Local Changes

No commit or push is performed automatically. Inspect the diff and stage only the files from this update, leaving unrelated work untouched:

```bash
git diff --check
git add -- CHANGELOG.md docs/project-portfolio.md \
  goalbot-frontend/src/data/projects.ts goalbot-frontend/src/data/timeline.ts \
  goalbot-frontend/src/views/Projects.vue goalbot-frontend/src/views/ProjectDetail.vue \
  goalbot-frontend/src/views/PublicBlog.vue goalbot-frontend/src/views/Journey.vue \
  goalbot-frontend/src/components/ProjectFlow.vue goalbot-frontend/src/components/ProjectNotes.vue \
  goalbot-frontend/src/components/PublicSiteHeader.vue goalbot-frontend/src/styles/portfolio.css \
  goalbot-frontend/src/router/index.ts goalbot-frontend/src/api/note.ts goalbot-frontend/src/api/request.ts \
  goalbot-frontend/tests/projects.test.mjs goalbot-frontend/tests/projects.browser.cjs
git diff --cached --stat
git commit -m "feat: add public project portfolio and linked reading"
git push origin main
```

The staging example uses Bash line continuations. In PowerShell, put the `git add` arguments on one line or use PowerShell backticks. Verify any previously staged files before committing.

## Update The Local Deployment Server

After the commit is available on `origin/main`, run on the local server hosting Docker, not on the overseas VPS. Do not run this concurrently with an automatic deploy job.

```bash
(
  set -e
  cd /home/glg/goalbot/Study-assistant
  git pull --ff-only origin main
  docker compose build goalbot-frontend
  docker compose up -d --no-deps --force-recreate goalbot-frontend
  docker compose ps goalbot-frontend
  curl -fsS -o /dev/null -w 'Local projects: HTTP %{http_code}\n' http://127.0.0.1:18080/projects
  curl -fsS -o /dev/null -w 'Local detail: HTTP %{http_code}\n' http://127.0.0.1:18080/projects/cloud-edge-capture
)
```

Keep the existing `.env` and MySQL volume. Do not run `docker compose down -v`. Only the frontend container is rebuilt and recreated.

Open `https://linge.xin/projects` and refresh a detail URL directly. Confirm the three projects, timeline selection and real related-note results. A local HTTP 200 verifies the SPA entry, not the rendered interface. If the public page is stale, inspect the served JS asset hash and Cloudflare HTML caching before assuming deployment succeeded; the origin container and public domain must serve the same build.
