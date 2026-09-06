const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const base = process.env.QA_BASE_URL || 'http://127.0.0.1:4173';
const out = process.env.QA_OUTPUT_DIR || path.join(os.tmpdir(), 'linge-lazy-ui-qa');
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
const user = { id: 1, username: 'qa-owner', nickname: '测试站长', role: 'ADMIN', status: 1, createdAt: '2026-09-06T12:00:00', updatedAt: '2026-09-06T12:00:00' };
const note = { id: 71, userId: 1, title: '隔离测试笔记', category: '技术/Java', content: '# 测试标题\n\n> [!note] 提示\n> 仅浏览器测试数据。\n\n$$E=mc^2$$', published: true, official: true, wordCount: 40, createdAt: user.createdAt, updatedAt: user.updatedAt };
const passes = [], errors = [], requests = [];
async function check(name, fn) { await fn(); passes.push(name); console.log('PASS ' + name); }

(async () => {
  await fs.mkdir(out, { recursive: true });
  const browser = await chromium.launch({ headless: true, ...(process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {}) });
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, reducedMotion: 'reduce' });
    const page = await context.newPage();
    page.on('pageerror', error => errors.push(error.message));
    page.on('console', message => { if (/Failed to resolve (component|directive)/.test(message.text())) errors.push(message.text()); });
    await context.route(url => url.origin === new URL(base).origin && url.pathname.startsWith('/api/'), async route => {
      const url = new URL(route.request().url()), method = route.request().method();
      requests.push({ path: url.pathname, method });
      const ok = data => route.fulfill({ json: { code: 0, data } });
      if (url.pathname === '/api/auth/login') return ok({ token: 'isolated-qa-token', expiresAt: '2027-01-01', user });
      if (url.pathname === '/api/auth/me') return ok(user);
      if (url.pathname === '/api/auth/logout') return ok(null);
      if (url.pathname === '/api/notes/categories') return ok([{ name: '技术/Java', count: 1 }]);
      if (url.pathname === '/api/notes' && method === 'GET') { await pause(450); return ok([note]); }
      if (url.pathname === '/api/notes/71' && method === 'GET') return ok(note);
      throw new Error('Unexpected API call in isolated UI test: ' + method + ' ' + url.pathname);
    });
    await check('Public home does not mount or request private controls', async () => {
      await page.goto(base + '/'); await page.locator('.hero-image').waitFor();
      assert.equal(await page.locator('.admin-shell').count(), 0);
      assert.deepEqual(requests, []);
      assert(!(await page.evaluate(() => performance.getEntriesByType('resource').some(item => /AdminLayout|Notebook/.test(item.name)))));
    });
    await check('Responsive sources stay sharp and the hero remains prioritized', async () => {
      for (const [width, height] of [[320, 780], [390, 844], [759, 360], [820, 1180], [1440, 1000]]) {
        await page.setViewportSize({ width, height });
        await page.reload();
        await page.locator('.hero-image').evaluate(img => img.decode());
        const data = await page.locator('.hero-image').evaluate(img => ({ src: img.currentSrc, priority: img.fetchPriority, loading: img.loading, width: img.naturalWidth, rendered: img.getBoundingClientRect().width, height: img.getBoundingClientRect().height }));
        assert(data.src.endsWith('.webp'));
        assert.equal(data.priority, 'high'); assert.equal(data.loading, 'eager');
        assert(data.width >= data.rendered * .85, 'image resolution unexpectedly small');
        assert.equal(data.src.includes('portrait'), width <= 759 && width / height <= 2 / 3);
        assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1));
      }
    });
    await check('Login validation, form styles and background load on demand', async () => {
      await page.locator('.site-workspace-link').click(); await page.waitForURL(base + '/login');
      await page.locator('.login-button').click();
      await page.getByText('请输入用户名', { exact: true }).waitFor();
      await page.getByText('请输入密码', { exact: true }).waitFor();
      assert.equal(await page.locator('.el-form-item__error').count(), 2);
      await page.mouse.move(0, 0);
      await page.locator('.login-button').blur();
      assert.equal(await page.locator('.login-button').evaluate(el => getComputedStyle(el).borderRadius), '6px');
      assert.equal(await page.locator('.login-button').evaluate(el => getComputedStyle(el).backgroundColor), 'rgb(40, 93, 77)');
      await page.locator('.context-image').evaluate(img => img.decode());
      assert((await page.locator('.context-image').evaluate(img => img.currentSrc)).endsWith('.webp'));
      await page.screenshot({ path: path.join(out, 'login-desktop.png') });
    });
    await check('Login mounts the lazy admin layout, identity fetch and loading directive', async () => {
      await page.getByLabel('用户名', { exact: true }).fill('qa-owner');
      await page.getByLabel('密码', { exact: true }).fill('isolated-test-password');
      await page.locator('.login-button').click(); await page.waitForURL(base + '/notebook');
      await page.locator('.admin-header').waitFor();
      await page.locator('.notebook-page > .el-loading-mask').waitFor();
      await page.locator('.notebook-page > .el-loading-mask').waitFor({ state: 'hidden' });
      assert(requests.some(item => item.path === '/api/auth/me'));
      assert((await page.locator('.account-button').textContent()).includes('测试站长'));
      assert(await page.evaluate(() => performance.getEntriesByType('resource').some(item => /AdminLayout/.test(item.name))));
    });
    await check('Account dropdown and password dialog retain styling and validation', async () => {
      await page.locator('.account-button').click();
      await page.getByRole('menuitem', { name: '修改密码' }).click();
      const dialog = page.getByRole('dialog', { name: '修改密码' });
      await dialog.waitFor();
      assert.equal(await dialog.locator('.el-dialog').evaluate(el => getComputedStyle(el).borderRadius), '10px');
      await dialog.getByRole('button', { name: '确认修改' }).click();
      await dialog.getByText('请输入当前密码', { exact: true }).waitFor();
      await dialog.getByRole('button', { name: '取消', exact: true }).click();
      await dialog.waitFor({ state: 'hidden' });
    });
    await check('Editor controls, math rendering and form validation survive on-demand imports', async () => {
      await page.getByRole('button', { name: '新建笔记', exact: true }).click();
      const dialog = page.locator('.notebook-editor-dialog'); await dialog.waitFor();
      await dialog.getByRole('button', { name: '保存笔记' }).click();
      await dialog.getByText('请输入笔记标题', { exact: true }).waitFor();
      await dialog.getByLabel('标题', { exact: true }).fill('不发送给真实后端');
      await dialog.locator('#note-markdown').fill('## 示例\n\n$$E=mc^2$$');
      await dialog.locator('.katex-display').waitFor();
      await dialog.locator('.el-radio-button').filter({ hasText: '预览' }).click();
      assert(await dialog.getByLabel('预览', { exact: true }).isChecked());
      assert.equal(await dialog.locator('.editor-workspace').getAttribute('class'), 'editor-workspace mode-preview');
      await page.screenshot({ path: path.join(out, 'editor-desktop.png') });
      await dialog.getByRole('button', { name: '取消', exact: true }).click();
    });
    await check('Notebook select, tooltip and confirmation dialog remain functional', async () => {
      await page.locator('.notebook-actions .el-radio-button').filter({ hasText: '列表' }).click();
      await page.locator('.notebook-reader .katex-display').waitFor();
      await page.locator('.library-filters .el-select').click();
      await page.getByRole('option', { name: '草稿', exact: true }).click();
      await page.locator('.reader-actions').getByRole('button', { name: '删除笔记', exact: true }).hover();
      await page.getByRole('tooltip', { name: '删除笔记' }).waitFor();
      await page.locator('.reader-actions').getByRole('button', { name: '删除笔记', exact: true }).click();
      const confirm = page.locator('.el-message-box'); await confirm.waitFor();
      assert(await confirm.evaluate(el => el.getBoundingClientRect().width > 250));
      await confirm.getByRole('button', { name: /^(取消|Cancel)$/ }).click();
      await confirm.waitFor({ state: 'hidden' });
      for (const action of ['close', 'escape']) {
        await page.locator('.reader-actions').getByRole('button', { name: '删除笔记', exact: true }).click();
        await confirm.waitFor();
        if (action === 'close') await confirm.locator('.el-message-box__headerbtn').click();
        else await page.keyboard.press('Escape');
        await confirm.waitFor({ state: 'hidden' });
      }
      assert(!requests.some(item => item.method === 'DELETE'));
    });
    await check('Returning from a public route reloads the correct private layout, then logout clears it', async () => {
      await page.locator('.admin-wordmark').click(); await page.waitForURL(base + '/');
      assert.equal(await page.locator('.admin-shell').count(), 0);
      await page.locator('.site-workspace-link').click(); await page.waitForURL(base + '/notebook');
      await page.locator('.account-button').click();
      await page.getByRole('menuitem', { name: '退出登录' }).click(); await page.waitForURL(base + '/login');
      assert.equal(await page.evaluate(() => localStorage.getItem('linge-owner-auth-token')), null);
      await page.goto(base + '/notebook'); await page.waitForURL(/\/login\?redirect=/);
    });
    await check('Repeat visits reuse resources and public route changes do not request private bundles', async () => {
      const cacheContext = await browser.newContext({ viewport: { width: 1440, height: 1000 }, reducedMotion: 'reduce' });
      const cachedPage = await cacheContext.newPage();
      const assetErrors = [];
      cachedPage.on('response', response => { if (response.url().includes('/assets/') && response.status() >= 400) assetErrors.push(response.url()); });
      await cachedPage.goto(base + '/'); await cachedPage.locator('.hero-image').evaluate(img => img.decode());
      const transferred = () => cachedPage.evaluate(() => performance.getEntriesByType('resource').reduce((sum, item) => sum + item.transferSize, 0));
      const cold = await transferred();
      await cachedPage.reload(); await cachedPage.locator('.hero-image').evaluate(img => img.decode());
      const warm = await transferred();
      assert(warm < cold * .5, `repeat visit transferred ${warm} bytes vs ${cold} cold bytes`);
      await cachedPage.locator('.site-nav').getByRole('link', { name: '项目', exact: true }).click();
      await cachedPage.waitForURL(base + '/projects');
      await cachedPage.goBack(); await cachedPage.locator('.hero-image').waitFor();
      assert.deepEqual(assetErrors, []);
      assert(!(await cachedPage.evaluate(() => performance.getEntriesByType('resource').some(item => /AdminLayout|Notebook/.test(item.name)))));
      await cacheContext.close();
    });
    assert.deepEqual(errors, []);
    assert.equal(await page.evaluate(() => [...document.querySelectorAll('*')].filter(el => el.tagName.startsWith('EL-')).length), 0);
    console.log(JSON.stringify({ passed: passes.length, screenshots: out, errors }, null, 2));
    await context.close();
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
