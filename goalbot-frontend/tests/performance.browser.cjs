// Run against a production preview, not Vite's development modules.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const { gzipSync } = require('node:zlib');
const base = process.env.QA_BASE_URL || 'http://127.0.0.1:4173';
const phase = process.env.QA_PERFORMANCE_PHASE || 'after';
const out = process.env.QA_OUTPUT_DIR || path.join(os.tmpdir(), 'linge-performance-qa');
const runs = Number(process.env.QA_RUNS || 3);
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
const median = values => [...values].sort((a, b) => a - b)[Math.floor(values.length / 2)];

(async () => {
  await fs.mkdir(out, { recursive: true });
  const browser = await chromium.launch({ headless: true, ...(process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {}) });
  const report = { phase, browser: browser.version(), base, runs, measurements: [] };
  try {
    for (const device of [
      { name: 'mobile', width: 390, height: 844, deviceScaleFactor: 2 },
      { name: 'desktop', width: 1440, height: 1000, deviceScaleFactor: 1 }
    ]) {
      const samples = [];
      for (let run = 0; run < runs; run++) {
        const context = await browser.newContext({ viewport: { width: device.width, height: device.height }, deviceScaleFactor: device.deviceScaleFactor, reducedMotion: 'reduce' });
        const page = await context.newPage();
        const errors = [], apiRequests = [];
        page.on('pageerror', error => errors.push(error.message));
        page.on('request', request => { if (new URL(request.url()).pathname.startsWith('/api/')) apiRequests.push(request.url()); });
        const cdp = await context.newCDPSession(page);
        await cdp.send('Network.enable');
        await cdp.send('Network.setCacheDisabled', { cacheDisabled: true });
        await cdp.send('Network.emulateNetworkConditions', { offline: false, latency: 40, downloadThroughput: 500000, uploadThroughput: 250000 });
        await cdp.send('Emulation.setCPUThrottlingRate', { rate: 3 });
        await page.addInitScript(() => {
          window.__metrics = { lcp: 0, cls: 0 };
          new PerformanceObserver(list => { for (const entry of list.getEntries()) window.__metrics.lcp = entry.startTime; }).observe({ type: 'largest-contentful-paint', buffered: true });
          new PerformanceObserver(list => { for (const entry of list.getEntries()) if (!entry.hadRecentInput) window.__metrics.cls += entry.value; }).observe({ type: 'layout-shift', buffered: true });
        });
        await page.goto(base + '/', { waitUntil: 'load', timeout: 60000 });
        await page.locator('.hero-image').waitFor();
        await page.locator('.hero-image').evaluate(img => img.decode());
        await page.evaluate(() => document.fonts.ready);
        await pause(700);
        const sample = await page.evaluate(() => ({
          ...window.__metrics,
          hero: document.querySelector('.hero-image').currentSrc,
          heroWidth: document.querySelector('.hero-image').naturalWidth,
          resources: performance.getEntriesByType('resource').filter(entry => entry.name.startsWith(location.origin)).map(entry => ({ name: new URL(entry.name).pathname, bytes: entry.encodedBodySize, decoded: entry.decodedBodySize, transfer: entry.transferSize })),
          overflow: document.documentElement.scrollWidth > innerWidth + 1
        }));
        assert.deepEqual(errors, []);
        assert.deepEqual(apiRequests, [], 'homepage must not request notes or owner data');
        assert.equal(sample.overflow, false);
        sample.jsBytes = sample.resources.filter(item => item.name.endsWith('.js')).reduce((sum, item) => sum + item.decoded, 0);
        sample.cssBytes = sample.resources.filter(item => item.name.endsWith('.css')).reduce((sum, item) => sum + item.decoded, 0);
        sample.imageBytes = sample.resources.filter(item => /\.(png|webp)$/.test(item.name)).reduce((sum, item) => sum + item.bytes, 0);
        sample.totalBodyBytes = sample.resources.reduce((sum, item) => sum + item.bytes, 0);
        sample.jsGzipBytes = 0;
        sample.cssGzipBytes = 0;
        for (const resource of sample.resources.filter(item => /\.(js|css)$/.test(item.name))) {
          const bytes = await fs.readFile(path.join(__dirname, '..', 'dist', resource.name));
          sample[resource.name.endsWith('.js') ? 'jsGzipBytes' : 'cssGzipBytes'] += gzipSync(bytes, { level: 6 }).length;
        }
        if (phase === 'after') {
          assert(sample.jsBytes < 250000, 'homepage JS exceeds 250 KB decoded budget');
          assert(sample.cssBytes < 60000, 'homepage CSS exceeds 60 KB decoded budget');
          assert(sample.imageBytes < 350000, 'hero image exceeds 350 KB budget');
          assert(sample.cls < .05, 'unexpected homepage layout shift');
          assert(sample.hero.endsWith('.webp'));
          assert(!sample.resources.some(item => /AdminLayout|Notebook|OfficialNotes|KaTeX|workspace-hero/.test(item.name)), 'private or reader resources loaded on home');
        }
        if (run === 0) await page.screenshot({ path: path.join(out, `${phase}-${device.name}.png`) });
        samples.push(sample);
        console.log(`${phase} ${device.name} run ${run + 1}: JS=${sample.jsBytes}, CSS=${sample.cssBytes}, image=${sample.imageBytes}, LCP=${Math.round(sample.lcp)}ms, CLS=${sample.cls}`);
        await context.close();
      }
      report.measurements.push({ device, throttle: { downloadBytesPerSecond: 500000, latencyMs: 40, cpuSlowdown: 3 }, medianLcpMs: Math.round(median(samples.map(sample => sample.lcp))), samples });
    }
    await fs.writeFile(path.join(out, `${phase}.json`), JSON.stringify(report, null, 2));
    console.log(JSON.stringify({ report: path.join(out, `${phase}.json`), medians: report.measurements.map(item => ({ device: item.device.name, lcpMs: item.medianLcpMs })) }, null, 2));
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
