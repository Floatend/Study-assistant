// Read-only browser fixtures. Reports and screenshots stay outside the repository.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const base = process.env.QA_BASE_URL || 'http://127.0.0.1:5173';
const phase = process.env.QA_LAYOUT_PHASE || 'after';
const out = process.env.QA_OUTPUT_DIR || path.join(os.tmpdir(), 'linge-reading-layout-qa', phase);
const widths = [320, 390, 759, 760, 820, 1023, 1024, 1100, 1101, 1279, 1280, 1440, 1920];
const code = 'const result = ' + 'request.withContext().validate().resolve().'.repeat(8) + 'execute();';
const math = Array.from({ length: 30 }, (_, i) => `\\frac{x_{${i}}^2}{1+x_{${i}}}`).join(' + ');
const body = '# 基础知识\n\n> [!note] 阅读提示\n> 隔离测试内容，不是公开文章。\n\n$$' + math + '$$\n\n```javascript\n' + code + '\n```\n\n' +
  '| 项目 | ' + Array.from({ length: 12 }, (_, i) => '第' + i + '项指标').join(' | ') + ' |\n' +
  '| --- | ' + Array(12).fill('---').join(' | ') + ' |\n' +
  '| 验证 | ' + Array(12).fill('需要完整阅读的内容').join(' | ') + ' |\n\n' +
  '用于验证笔记正文宽度、滚动与目录定位的隔离测试文字。\n\n'.repeat(25) + '## 推导过程\n\n' +
  '这是下一节正文，验证跳转以后标题不被导航遮挡。\n\n'.repeat(30) + '## 结论\n\n验证完成。';
const records = [71, 72].map(id => ({ id, userId: 1, title: id === 71 ? '大学物理：电磁场与边界条件的推导，以及 Java 示例中的长行展示' : '后续阅读', category: '课程/物理', summary: '', content: body, wordCount: 2000, published: true, official: true, authorName: '测试作者', createdAt: '2026-09-07T10:00:00', updatedAt: '2026-09-07T10:00:00' }));
let failList = false, failArticle = false;
const errors = [], requests = [], passed = [], measurements = [];
async function check(name, fn) { await fn(); passed.push(name); console.log('PASS ' + name); }
async function article(page) {
  await page.goto(base + '/notes?note=71');
  await page.locator('.article-body .katex').first().waitFor();
  await page.waitForFunction(() => document.activeElement?.id === 'reader-article-title');
}
async function visible(locator) { return await locator.count() > 0 && await locator.isVisible(); }
async function noOverflow(page) {
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), 'page overflow at ' + page.url());
}

(async () => {
  await fs.mkdir(out, { recursive: true });
  const browser = await chromium.launch({ headless: true, ...(process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {}) });
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, reducedMotion: 'reduce' });
    const page = await context.newPage();
    page.on('pageerror', error => errors.push(error.message));
    await context.route(url => url.origin === new URL(base).origin && url.pathname.startsWith('/api/'), async route => {
      const url = new URL(route.request().url());
      requests.push(url.pathname);
      assert.equal(route.request().method(), 'GET');
      const ok = data => route.fulfill({ json: { code: 0, data } });
      const fail = () => route.fulfill({ status: 503, json: { code: 503, message: 'Isolated fixture error' } });
      if (url.pathname === '/api/public/notes/categories') return ok([{ name: '课程/物理', count: 2 }]);
      if (url.pathname === '/api/public/notes/search') {
        if (failList) return fail();
        const keyword = url.searchParams.get('keyword') || '';
        const items = records.filter(item => !keyword || item.title.includes(keyword)).map(({ content, userId, ...item }) => ({ ...item, excerpt: '布局测试摘要' }));
        return ok({ items, total: items.length, page: 1, pageSize: 12 });
      }
      if (url.pathname.endsWith('/related')) return ok([records[1]]);
      if (url.pathname.endsWith('/navigation')) return ok({ previous: null, next: records[1], position: 1 });
      const item = records.find(record => url.pathname === '/api/public/notes/' + record.id);
      if (item) return failArticle ? fail() : ok(item);
      throw new Error('Unexpected API request: ' + url.pathname);
    });

    await check('Homepage hierarchy, stable hit areas and original destinations', async () => {
      for (const width of [320, 390, 760, 1101, 1440]) {
        await page.setViewportSize({ width, height: 900 });
        await page.goto(base + '/');
        await page.locator('.hero-image').evaluate(img => img.decode());
        await noOverflow(page);
        assert.deepEqual(requests, []);
        assert(await page.evaluate(strict => [...document.querySelectorAll('.site-header a')].every(el => {
          const r = el.getBoundingClientRect(); return r.left >= 0 && r.right <= innerWidth && (!strict || r.height >= 44);
        }), phase === 'after'));
        const primary = page.locator('.hero-actions a').first(), owner = page.locator('.site-workspace-link');
        assert.equal(await owner.getAttribute('href'), '/login');
        assert.equal(await primary.getAttribute('href'), '#selected-work');
        if (phase === 'after') {
          assert(!(await owner.getAttribute('class')).includes('liquid-glass'));
          assert.notEqual(await primary.evaluate(el => getComputedStyle(el).backgroundColor), 'rgba(0, 0, 0, 0)');
          const before = await primary.boundingBox();
          await primary.hover();
          assert.deepEqual(await primary.boundingBox(), before, 'hover must not move the CTA');
        }
        await page.mouse.move(0, 0);
        await page.screenshot({ path: path.join(out, `home-${width}.png`) });
      }
      await page.locator('.hero-actions a').first().click();
      assert.equal(await page.evaluate(() => document.activeElement?.id), 'selected-work');
      await page.goto(base + '/');
      await page.locator('.hero-actions').getByRole('link', { name: '阅读笔记' }).click();
      await page.waitForURL(base + '/notes');
    });

    await check('Reader keeps local scroll containers and useful widths at every breakpoint', async () => {
      for (const width of widths) {
        await page.setViewportSize({ width, height: 900 });
        await article(page);
        await noOverflow(page);
        const metrics = await page.locator('.article-body').evaluate(el => ({ width: el.getBoundingClientRect().width, top: el.getBoundingClientRect().top }));
        const library = await visible(page.locator('.knowledge-library'));
        const outline = await visible(page.locator('.article-outline'));
        const tools = await visible(page.locator('.reader-tools'));
        measurements.push({ viewport: width, bodyWidth: metrics.width, library, outline, tools });
        if (phase === 'after') {
          assert.equal(library, width >= 1280);
          assert.equal(outline, width >= 1024);
          assert.equal(tools, width < 1280);
          assert.equal(await visible(page.locator('.reader-tools').getByRole('button', { name: '目录', exact: true })), width < 1024);
          if (width >= 1024) assert(metrics.width >= 640 && metrics.width <= 761, JSON.stringify(measurements.at(-1)));
          for (const selector of ['.math-display', '.code-block-shell pre', '.markdown-table-wrap']) {
            const container = page.locator('.article-body ' + selector);
            const data = await container.evaluate(el => {
              el.scrollLeft = 120;
              return { client: el.clientWidth, scroll: el.scrollWidth, offset: el.scrollLeft, overflow: getComputedStyle(el).overflowX };
            });
            assert.equal(data.overflow, 'auto', selector + ' must contain wide content');
            assert(data.scroll > data.client, selector + ' fixture must overflow locally');
            assert(data.offset > 0, selector + ' cannot scroll');
            await container.evaluate(el => { el.scrollLeft = 0; });
          }
        }
        if ([320, 390, 820, 1024, 1101, 1280, 1440, 1920].includes(width)) await page.screenshot({ path: path.join(out, `reader-${width}.png`) });
      }
    });

    if (phase === 'before') {
      await fs.writeFile(path.join(out, 'measurements.json'), JSON.stringify(measurements, null, 2));
      console.log(JSON.stringify({ phase, measurements, out }, null, 2));
      return;
    }

    await check('List and reading layouts keep category access on intermediate screens', async () => {
      for (const width of [320, 1023, 1024, 1101, 1279, 1280]) {
        await page.setViewportSize({ width, height: 900 });
        await page.goto(base + '/notes');
        await page.locator('.note-results .public-note-link').first().waitFor();
        await noOverflow(page);
        assert.equal(await visible(page.locator('.knowledge-library')), width >= 1024);
        assert.equal(await visible(page.locator('.reader-tools')), width < 1024);
        assert.equal(await page.locator('.article-outline').count(), 0);
        await page.locator('.note-results .public-note-link').first().click();
        await page.locator('.article-body').waitFor();
        if (width < 1280) {
          await page.locator('.reader-tools').getByRole('button', { name: '笔记', exact: true }).click();
          const drawer = page.getByRole('dialog');
          await drawer.locator('.category-tree-label').getByText('课程', { exact: true }).click();
          await drawer.waitFor({ state: 'hidden' });
          await page.locator('.note-results .public-note-link').first().waitFor();
          assert.equal(new URL(page.url()).searchParams.get('category'), '课程');
          await page.goBack();
          await page.locator('.article-body').waitFor();
        }
      }
    });

    await check('Inline and drawer outlines place focus below the header', async () => {
      for (const width of [390, 1024, 1101, 1280]) {
        await page.setViewportSize({ width, height: 900 });
        await article(page);
        if (width < 1024) {
          await page.locator('.reader-tools').getByRole('button', { name: '目录', exact: true }).click();
          await page.getByRole('dialog').getByRole('button', { name: '推导过程', exact: true }).click();
          await page.getByRole('dialog').waitFor({ state: 'hidden' });
        } else await page.locator('.article-outline').getByRole('button', { name: '推导过程', exact: true }).click();
        await page.waitForFunction(() => {
          const el = document.activeElement, offset = parseFloat(getComputedStyle(document.querySelector('.knowledge-page')).getPropertyValue('--reader-offset'));
          return el?.textContent === '推导过程' && Math.abs(el.getBoundingClientRect().top - offset) < 3;
        });
        if (width < 1280) {
          const opener = page.locator('.reader-tools').getByRole('button', { name: '笔记', exact: true });
          await opener.click(); await page.keyboard.press('Escape');
          await page.getByRole('dialog').waitFor({ state: 'hidden' });
          assert(await opener.evaluate(el => el === document.activeElement));
        }
      }
    });

    await check('Search, list context and history survive the new column transitions', async () => {
      await page.setViewportSize({ width: 1101, height: 900 });
      await page.goto(base + '/notes');
      const input = page.locator('.note-results').getByRole('textbox', { name: '搜索学习笔记' });
      await input.fill('大学物理'); await input.press('Enter');
      await page.waitForFunction(() => document.querySelectorAll('.note-results .public-note-link').length === 1);
      await page.locator('.note-results .public-note-link').click();
      await page.locator('.article-body').waitFor();
      await page.setViewportSize({ width: 1280, height: 900 });
      await noOverflow(page);
      await page.getByRole('button', { name: '返回列表', exact: true }).click();
      assert.equal(await input.inputValue(), '大学物理');
      await page.goBack(); await page.locator('.article-body').waitFor();
      assert.equal(new URL(page.url()).searchParams.get('q'), '大学物理');
    });

    await check('Independent failures still have reachable retry controls at 1101px', async () => {
      await page.setViewportSize({ width: 1101, height: 900 });
      failList = true;
      await article(page);
      await page.locator('.reader-tools').getByRole('button', { name: '笔记', exact: true }).click();
      await page.getByRole('dialog').getByText('文章列表暂时不可用，请重试。').waitFor();
      failList = false;
      await page.getByRole('dialog').getByRole('button', { name: '重试', exact: true }).click();
      await page.getByRole('dialog').locator('.public-note-link').first().waitFor();
      await page.keyboard.press('Escape');
      failArticle = true;
      await page.reload();
      await page.locator('.knowledge-empty').getByRole('button', { name: '重新加载' }).waitFor();
      await noOverflow(page);
      failArticle = false;
      await page.locator('.knowledge-empty').getByRole('button', { name: '重新加载' }).click();
      await page.locator('.article-body').waitFor();
      assert(await page.locator('.article-outline').isVisible());
    });

    await check('Math, callouts and code-copy behavior remain intact', async () => {
      await page.setViewportSize({ width: 1101, height: 900 });
      await article(page);
      assert.equal(await page.locator('.article-body .katex-error').count(), 0);
      await page.locator('.article-body').getByText('阅读提示', { exact: true }).waitFor();
      // Capture copy requests inside this isolated page; leave the user's clipboard unchanged.
      await page.evaluate(() => Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: async value => { window.__copiedCode = value; } } }));
      await page.getByRole('button', { name: '复制代码', exact: true }).click();
      await page.waitForFunction(() => typeof window.__copiedCode === 'string');
      assert.equal((await page.evaluate(() => window.__copiedCode)).trimEnd(), code);
    });

    await check('Landscape and reduced CSS viewport remain usable without clipped tools', async () => {
      // A 720px CSS viewport covers the reflow width of 1440px at 200% zoom; this is not a native zoom test.
      for (const [width, height] of [[844, 390], [720, 500]]) {
        await page.setViewportSize({ width, height });
        await article(page);
        await noOverflow(page);
        const toolbar = await page.locator('.reader-tools').boundingBox();
        assert(toolbar.x >= 0 && toolbar.x + toolbar.width < width - 60 && toolbar.y + toolbar.height <= height);
        await page.screenshot({ path: path.join(out, `reader-${width}x${height}.png`) });
      }
    });

    assert.deepEqual(errors, []);
    await fs.writeFile(path.join(out, 'measurements.json'), JSON.stringify(measurements, null, 2));
    console.log(JSON.stringify({ passed: passed.length, measurements, out, errors }, null, 2));
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
