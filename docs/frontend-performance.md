# Frontend Performance

## Scope

This phase preserves the existing layout, typography, palette, motion and original image composition. No SEO redesign, business API or database change is included.

- Replace global Element Plus registration/full CSS with the standard `unplugin-vue-components` Element Plus resolver. Component CSS and `v-loading` are imported where used; message/message-box service styles are imported explicitly.
- Move the existing private shell into `src/layouts/AdminLayout.vue`, loaded only on private routes. Mount the app after `router.isReady()` so initial navigation cannot temporarily select the private branch and download its forms on the public homepage.
- Increase the specificity of existing site UI overrides so lazy-loaded library CSS cannot change button radii, backgrounds or dialog styling.
- Generate WebP derivatives with `npm run optimize:images`. Original PNGs remain untouched in `src/assets`; only referenced WebP files enter the production build. Narrow portrait sources remove the horizontal area already clipped by `object-fit: cover`; landscape devices retain wide sources. The homepage hero stays eager/high-priority, with responsive source selection and stable dimensions.
- Enable gzip in the frontend container's Nginx for static text formats. HTML uses `Cache-Control: no-cache` (revalidation), while hashed assets use a one-year immutable cache. Existing API proxy headers and routing are unchanged. VPS Nginx and FRP require no changes.
- Handle cancellation/dismissal of the notebook delete confirmation without an unhandled rejection, discovered during development-server regression checks. Cancelling still leaves the note unchanged and sends no delete request.

## Measured Results

Baseline: commit `82ab4e8`. Both versions used production builds served by Vite preview, Chrome 153 on this Windows machine, empty browser contexts with cache disabled, 500,000 bytes/s download (4 Mbps), 40ms latency, 3x CPU slowdown and reduced motion. Three cold runs per viewport; LCP values below are medians. Mobile is 390x844 at DPR 2; desktop is 1440x1000 at DPR 1.

| Metric | Before | After |
| --- | ---: | ---: |
| Homepage JavaScript, decoded | 1,111,981 B | 174,682 B |
| Homepage CSS, decoded | 378,294 B | 26,989 B |
| Homepage JavaScript, gzip level 6 estimate | 371,343 B | 68,354 B |
| Homepage CSS, gzip level 6 estimate | 52,827 B | 6,619 B |
| Mobile hero image | 2,841,840 B | 162,060 B |
| Desktop hero image | 2,841,840 B | 327,508 B |
| Mobile initial resource bodies, encoded | 3,266,010 B | 237,033 B |
| Desktop initial resource bodies, encoded | 3,266,010 B | 402,481 B |
| Mobile LCP median | 7,280 ms | 856 ms |
| Desktop LCP median | 7,276 ms | 1,200 ms |
| Observed homepage CLS | 0 | 0 |

Encoded resource totals exclude the document and HTTP headers. Vite preview already uses compression, so these measurements do not claim an additional measured benefit from enabling production Nginx gzip. They are controlled local lab results, not Lighthouse scores or a promise of the same timing through Cloudflare/VPS/FRP. Measure the deployed site separately.

The login background also falls from 1,784,419 B to 80,122 B at full source resolution. Smaller WebP candidates are 97,414 B (portrait hero), 196,360 B (wide hero) and 40,804 B (workspace). Browsers choose a source according to viewport, rendered cover size and pixel density.

## Verification

- Production Vue TypeScript/Vite build and 10 unit tests pass.
- 15 project browser checks and all 13 prior note-discovery checks pass against the production preview.
- 9 new lazy-UI checks cover public/private resource separation, portrait/landscape imagery, login validation/styles, loading directives, identity fetch, password dialog, editor controls, math preview, select/tooltip/confirmation behavior, logout and warm resource reuse. All auth/note interactions in these tests use isolated browser fixtures, never live MySQL.
- Before/after screenshots verify the public homepage composition at desktop and mobile sizes. Existing VueUse annotation warnings and the reader-only large chunk warning remain; Markdown, syntax highlighting and KaTeX are not loaded on the homepage.
- Docker is not running on this development machine. Container-level `nginx -t`, actual production response headers and live backend integration have not been verified here; perform the checks below during deployment.

From `goalbot-frontend`:

```bash
npm ci
npm run test:unit
npm run build
npm run preview -- --host 127.0.0.1
```

Image generation is a maintenance command, not a requirement during each Docker build. Generated WebP derivatives are checked into the repository:

```bash
npm run optimize:images
```

With Playwright and Chrome available, in another terminal:

```bash
export PLAYWRIGHT_MODULE=/absolute/path/to/node_modules/playwright
export CHROME_PATH=/absolute/path/to/chrome
export QA_BASE_URL=http://127.0.0.1:4173
node tests/projects.browser.cjs
node tests/lazy-ui.browser.cjs
QA_PERFORMANCE_PHASE=after node tests/performance.browser.cjs
```

The performance suite enforces decoded homepage budgets of 250 KB JS and 60 KB CSS, a 350 KB hero-image budget and CLS below 0.05. It also rejects private/reader resource requests on the homepage. `QA_RUNS` defaults to 3; screenshots/reports go to the system temp directory under `linge-performance-qa`, not the project tree. A baseline run uses `QA_PERFORMANCE_PHASE=before` against an unmodified production build.

## Commit And Deploy

No commit, push or remote deployment is performed automatically. Check the file list before staging; do not add unrelated temporary files:

```bash
git status --short
git add -- goalbot-frontend CHANGELOG.md docs/frontend-performance.md
git diff --cached --stat
git commit -m "perf: reduce public page assets and lazy-load owner UI"
git push origin main
```

Run on the local server hosting Docker after the push completes. Do not run simultaneously with an automatic deployment job. The existing backend must be running for the one-off Nginx config check to resolve its upstream service name.

```bash
(
  set -e
  cd /home/glg/goalbot/Study-assistant
  git pull --ff-only origin main
  docker compose build goalbot-frontend
  docker compose run --rm --no-deps goalbot-frontend nginx -t
  docker compose up -d --no-deps --force-recreate goalbot-frontend
  docker compose ps goalbot-frontend
  curl -fsSI http://127.0.0.1:18080/projects
)
```

Expect HTML `Cache-Control: no-cache`. Check the built JS response with an actual GET (not just HEAD) so gzip is exercised:

```bash
cd /home/glg/goalbot/Study-assistant
ASSET=$(docker compose exec -T goalbot-frontend sh -c 'basename /usr/share/nginx/html/assets/index-*.js')
curl -fsS -D - -o /dev/null -H 'Accept-Encoding: gzip' "http://127.0.0.1:18080/assets/$ASSET"
```

Expect `Content-Encoding: gzip`, `Vary: Accept-Encoding` and a long-lived immutable asset cache. Missing hashed assets should return 404 rather than the SPA HTML. Verify homepage, login, notes and direct project URLs through `https://linge.xin/` as well.

Keep `.env`, MySQL volumes and FRP configuration intact; no SQL or backend rebuild is required. Cloudflare rules that override origin HTML caching must be adjusted to respect revalidation, or old HTML can still survive at the edge. An already-open tab can still reference an older deployment's chunks; refresh it after deployment rather than force-reloading users with unsaved edits.
