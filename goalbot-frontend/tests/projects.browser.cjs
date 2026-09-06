// Isolated public API fixtures only. This suite never connects to MySQL.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const base = process.env.QA_BASE_URL || 'http://127.0.0.1:5173';
const out = process.env.QA_OUTPUT_DIR || path.join(os.tmpdir(), 'linge-projects-qa');
const ids = ['cloud-edge-capture', 'wechat-llm-agent', 'ceramic-commerce'];
const titles = ['云边端智能识别抓取平台', '基于 LLM 的微信智能体机器人', '3D 定制陶瓷电商平台'];
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
const article = { id: 71, userId: 1, title: 'Spring Boot 测试笔记', category: '技术/Java', content: '# 接口设计\n\n> [!note] 测试提示\n> 仅浏览器隔离数据。\n\n$$E=mc^2$$\n\n' + '这里是布局验证用的笔记正文。\n\n'.repeat(60) + '## 部署\n\n测试结束。', wordCount: 1600, updatedAt: '2026-09-06T12:00:00', createdAt: '2026-09-06T12:00:00', published: true, official: true };
let failure = false, empty = false, unsafe = false, delay = 0;
let requests = [];
const errors = [], passes = [];
async function check(name, fn) { await fn(); passes.push(name); console.log('PASS ' + name); }
async function detail(page, index) { await page.goto(base + '/projects/' + ids[index]); await page.getByRole('heading', { level: 1, name: titles[index], exact: true }).waitFor(); }
async function settleNotes(page) { await page.waitForFunction(() => document.querySelector('.notes-results')?.getAttribute('aria-busy') === 'false'); }
async function noOverflow(page) {
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), 'page overflow at ' + page.url());
  assert(await page.evaluate(() => [...document.querySelectorAll('.site-header a')].every(el => { const r = el.getBoundingClientRect(); return r.width > 0 && r.left >= 0 && r.right <= innerWidth + 1; })), 'header links clipped');
}

(async () => {
  await fs.mkdir(out, { recursive: true });
  const browser = await chromium.launch({ headless: true, ...(process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {}) });
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, reducedMotion: 'reduce' });
    const page = await context.newPage();
    page.on('pageerror', error => errors.push(error.message));
    await context.route('**/api/public/notes**', async route => {
      const url = new URL(route.request().url()), pathname = url.pathname;
      requests.push(url.href);
      const ok = data => route.fulfill({ json: { code: 0, data } });
      if (pathname.endsWith('/categories')) return ok([{ name: '技术/Java', count: 1 }]);
      if (pathname.endsWith('/search')) {
        const keyword = url.searchParams.get('keyword') || 'Spring Boot', wait = delay, fail = failure, noItems = empty;
        if (wait) await pause(wait);
        if (fail) return route.fulfill({ status: fail === 'business' ? 200 : 503, json: { code: 503, message: 'Isolated QA failure' } });
        return ok({ items: noItems ? [] : [{ ...article, title: unsafe ? '<img src=x onerror=alert(1)> 测试标题' : keyword + ' 测试笔记' }], total: noItems ? 0 : 1, page: 1, pageSize: Number(url.searchParams.get('pageSize')) || 12 });
      }
      if (pathname.endsWith('/related')) return ok([]);
      if (pathname.endsWith('/navigation')) return ok({ previous: null, next: null, position: 1 });
      return ok(article);
    });

    await check('Overview is public, contains three genuine projects, and needs no note API', async () => {
      requests = [];
      await page.goto(base + '/projects');
      await page.getByRole('heading', { level: 1, name: '项目', exact: true }).waitFor();
      assert.equal(await page.locator('.portfolio-entry').count(), 3);
      assert.equal(requests.length, 0);
      for (const title of titles) assert(await page.getByRole('heading', { level: 2, name: title, exact: true }).isVisible());
      await page.screenshot({ path: path.join(out, 'projects-desktop.png'), fullPage: true });
      await page.goto(base + '/notebook');
      await page.waitForURL(/\/login\?redirect=/);
      await page.goto(base + '/projects');
      await page.getByRole('heading', { level: 1, name: '项目', exact: true }).waitFor();
    });
    await check('Homepage and main navigation reach the new project pages', async () => {
      await page.goto(base + '/');
      await page.locator('.project-content a').first().waitFor();
      for (const id of ids) assert.equal(await page.locator(`.project-content a[href="/projects/${id}"]`).count(), 1);
      await page.locator('.site-nav').getByRole('link', { name: '项目', exact: true }).click();
      await page.waitForURL(base + '/projects');
      await page.getByRole('link', { name: '查看项目详情：' + titles[0], exact: true }).click();
      await page.waitForURL(base + '/projects/' + ids[0]);
      assert(await page.locator('.site-nav a.is-current').getAttribute('href') === '/projects');
    });
    await check('Project content loads before optional notes; results use only the public search API', async () => {
      delay = 1400;
      await detail(page, 0);
      assert(await page.getByRole('heading', { name: '项目背景', exact: true }).isVisible());
      assert.equal(await page.locator('.notes-results').getAttribute('aria-busy'), 'true');
      await settleNotes(page);
      assert((await page.locator('.notes-list').textContent()).includes('Spring Boot 测试笔记'));
      const search = new URL(requests.filter(url => url.includes('/search')).at(-1));
      assert.equal(search.searchParams.get('pageSize'), '3');
      delay = 0;
    });
    await check('Each deep link has its own facts, narrative and deliverables', async () => {
      for (let index = 0; index < ids.length; index++) {
        await detail(page, index);
        await settleNotes(page);
        assert.equal(await page.locator('.deliverable-list li').count(), index === 0 ? 4 : 3);
        assert((await page.locator('.project-facts').textContent()).includes(index === 1 ? '个人开发' : '后端负责人'));
        assert.equal(await page.locator('.project-flow li').count(), 3);
      }
      await page.screenshot({ path: path.join(out, 'project-detail-desktop.png'), fullPage: true });
    });
    await check('Previous, next and browser history retain the correct project', async () => {
      await detail(page, 0);
      await page.locator('.project-neighbors a').click();
      await page.waitForURL(base + '/projects/' + ids[1]);
      assert.equal(await page.locator('.project-neighbors a').count(), 2);
      await page.locator('.project-neighbors a').last().click();
      await page.waitForURL(base + '/projects/' + ids[2]);
      assert.equal(await page.locator('.project-neighbors a').count(), 1);
      await page.goBack();
      await page.getByRole('heading', { level: 1, name: titles[1], exact: true }).waitFor();
      await page.goForward();
      await page.getByRole('heading', { level: 1, name: titles[2], exact: true }).waitFor();
      await settleNotes(page);
    });
    await check('Unknown project shows a recovery link and does not request notes', async () => {
      requests = [];
      await page.goto(base + '/projects/not-a-project');
      await page.getByRole('heading', { level: 1, name: '这个项目还没有公开详情' }).waitFor();
      assert.equal(requests.length, 0);
      await page.getByRole('link', { name: '返回项目总览' }).click();
      await page.waitForURL(base + '/projects');
    });
    await check('Timeline links in both directions select the intended project', async () => {
      await detail(page, 1);
      await page.getByRole('link', { name: '时间线定位' }).click();
      await page.waitForURL(/\/journey\?project=wechat-llm-agent/);
      assert.equal(await page.locator('.project-bar[aria-pressed="true"]').count(), 1);
      assert((await page.locator('.journey-inspector h2').textContent()).includes(titles[1]));
      await page.locator('.journey-inspector').getByRole('link', { name: '查看项目详情' }).click();
      await page.waitForURL(base + '/projects/' + ids[1]);
      await page.goBack();
      assert((await page.locator('.journey-inspector h2').textContent()).includes(titles[1]));
      await page.goto(base + '/journey?project=invalid');
      assert((await page.locator('.journey-inspector h2').textContent()).includes('个人网站'));
    });
    await check('Note failures remain local and recover on retry', async () => {
      failure = true;
      await detail(page, 0); await settleNotes(page);
      assert(await page.getByText('笔记暂时无法加载，项目内容仍可正常浏览。').isVisible());
      assert.equal(await page.locator('.el-message').count(), 0);
      failure = false;
      await page.locator('.project-notes').getByRole('button', { name: '重试' }).click();
      await settleNotes(page);
      assert.equal(await page.locator('.notes-list li').count(), 1);
    });
    await check('Empty notes and topic changes do not invent article results', async () => {
      empty = true;
      await page.locator('.notes-topic select').selectOption('部署'); await settleNotes(page);
      assert(await page.getByText('暂时没有“部署”主题的公开笔记。').isVisible());
      empty = false;
      await page.locator('.notes-topic select').selectOption('AI'); await settleNotes(page);
      assert((await page.locator('.notes-list').textContent()).includes('AI 测试笔记'));
    });
    await check('Business errors also honor silent auxiliary loading', async () => {
      failure = 'business';
      await page.locator('.notes-topic select').selectOption('部署'); await settleNotes(page);
      assert(await page.getByText('笔记暂时无法加载，项目内容仍可正常浏览。').isVisible());
      assert.equal(await page.locator('.el-message').count(), 0);
      failure = false;
      await page.locator('.project-notes').getByRole('button', { name: '重试' }).click(); await settleNotes(page);
    });
    await check('Note titles render as escaped text, never raw HTML', async () => {
      unsafe = true;
      await page.locator('.notes-topic select').selectOption('AI'); await settleNotes(page);
      assert.equal(await page.locator('.notes-list img').count(), 0);
      assert((await page.locator('.notes-list').textContent()).includes('<img src=x onerror=alert(1)>'));
      unsafe = false;
    });
    await check('Slow responses cannot replace a new topic or adjacent project', async () => {
      delay = 1200;
      await page.locator('.notes-topic select').selectOption('部署');
      await page.waitForFunction(() => document.querySelector('.notes-results')?.getAttribute('aria-busy') === 'true');
      delay = 0;
      await page.locator('.notes-topic select').selectOption('Spring Boot'); await settleNotes(page);
      await pause(1400);
      assert((await page.locator('.notes-list').textContent()).includes('Spring Boot 测试笔记'));
      delay = 1200;
      await page.locator('.notes-topic select').selectOption('AI');
      delay = 0;
      await page.locator('.project-neighbors a').last().click();
      await page.waitForURL(base + '/projects/' + ids[1]); await settleNotes(page);
      await pause(1400);
      assert((await page.locator('.notes-list').textContent()).includes('LLM 测试笔记'));
    });
    await check('Related notes reach real reader route and preserve search topic', async () => {
      await detail(page, 0); await settleNotes(page);
      await page.locator('.notes-list a').first().click();
      await page.waitForURL(/\/notes\?note=71&q=Spring\+Boot/);
      await page.locator('#reader-article-title').waitFor();
      assert.equal(await page.locator('.katex-display').count(), 1);
      await page.goBack();
      await page.getByRole('heading', { level: 1, name: titles[0], exact: true }).waitFor();
    });
    await check('Responsive pages and keyboard section targets clear sticky navigation', async () => {
      for (const width of [320, 390, 820, 1440]) {
        await page.setViewportSize({ width, height: 900 });
        await page.goto(base + '/projects'); await page.locator('.portfolio-entry').first().waitFor(); await noOverflow(page);
        await detail(page, 0); await settleNotes(page); await noOverflow(page);
        const anchor = page.locator('.project-outline a[href="#work"]');
        await anchor.focus(); await page.keyboard.press('Enter');
        assert(await page.evaluate(() => document.activeElement?.id === 'work'));
        assert(await page.evaluate(() => document.querySelector('#work').getBoundingClientRect().top >= document.querySelector('.site-header').getBoundingClientRect().bottom));
        await page.screenshot({ path: path.join(out, `detail-work-${width}.png`) });
        await page.goto(base + '/notes?note=71'); await page.locator('#reader-article-title').waitFor(); await noOverflow(page);
        assert(await page.evaluate(() => document.querySelector('#reader-article-title').getBoundingClientRect().top >= document.querySelector('.site-header').getBoundingClientRect().bottom));
      }
      await page.setViewportSize({ width: 390, height: 844 });
      await page.goto(base + '/projects');
      await page.locator('.portfolio-entry').first().waitFor();
      await page.screenshot({ path: path.join(out, 'projects-mobile.png'), fullPage: true });
      await detail(page, 1); await settleNotes(page);
      await page.screenshot({ path: path.join(out, 'project-detail-mobile.png'), fullPage: true });
    });
    await check('Normal motion and reduced motion both keep content usable', async () => {
      await page.emulateMedia({ reducedMotion: 'no-preference' });
      await page.goto(base + '/projects');
      await page.locator('.portfolio-entry').last().scrollIntoViewIfNeeded();
      await page.waitForFunction(() => Number(getComputedStyle(document.querySelector('.portfolio-entry:last-child')).opacity) > .9);
      await noOverflow(page);
      await page.emulateMedia({ reducedMotion: 'reduce' });
      assert.equal(await page.locator('.portfolio-entry').last().evaluate(el => getComputedStyle(el).animationName), 'none');
    });
    assert.deepEqual(errors, []);
    console.log(JSON.stringify({ passed: passes.length, screenshots: out, browserErrors: errors }, null, 2));
    await context.close();
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
