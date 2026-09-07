# 前端改进 A：入口层级与阅读布局

日期：2026-09-07  
基线提交：`7e4e98d`  
状态：本地实现与回归完成，尚未提交、推送或部署。

## 本批变化

- 公开站属于 Explore（浏览）界面。顶部站长入口保留原地址，去掉玻璃胶囊背景，使用普通链接和较轻字重；文字仍使用深色品牌 token，避免用低对比度表达次要层级。
- 首页“查看项目”成为实色主要入口，“阅读笔记”保持次要链接。锚点位置、内容顺序、全部三个项目、樱花构图和遮罩均未改变。
- 站点名称与导航入口保持至少 44px 点击高度。主要入口悬停不改变尺寸，减少动态模式关闭其位移。
- 文章阅读在 1024px 以下为单栏；1024–1279px 显示正文与目录，笔记分类通过浮动工具打开；1280px 起显示三栏。
- 笔记列表采用独立规则：1024px 起显示分类与结果两栏，不预留正文目录区域。布局切换不会改变现有查询参数和阅读恢复数据。
- 限制正文最大宽度，避免在宽屏上每行过长；宽表格单元格保留最小可读宽度，表格、代码和公式在自己的区域内横向滚动。
- 将以前位于临时目录的笔记回归脚本整理为可复用的仓库测试，扩展中屏覆盖；登录样式测试等待 hover 过渡结束，避免捕获过渡中的临时颜色。

## 正文宽度对比

以下是同一隔离文章的 `.article-body` 实测 CSS 像素宽度，不包含侧栏和正文内边距。

| 视口宽度 | 改进前 | 改进后 | 新阅读布局 |
| --- | ---: | ---: | --- |
| 390px | 326px | 326px | 单栏 |
| 820px | 692px | 692px | 单栏 |
| 1023px | 895px | 728px | 单栏，限制最大行长 |
| 1024px | 896px | 752px | 正文 + 目录 |
| 1100px | 972px | 760px | 正文 + 目录 |
| 1101px | 501px | 760px | 正文 + 目录 |
| 1279px | 679px | 760px | 正文 + 目录 |
| 1280px | 680px | 740px | 分类 + 正文 + 目录 |
| 1440px | 840px | 760px | 分类 + 正文 + 目录 |
| 1920px | 904px | 760px | 分类 + 正文 + 目录 |

## 验证结果

- `npm run build` 和 10 项单元测试通过。
- 8 项新的入口与阅读布局检查通过，覆盖 320、390、759、760、820、1023、1024、1100、1101、1279、1280、1440、1920px，以及 844×390 横屏。
- 13 项笔记功能回归、15 项项目回归、9 项按需加载和站长交互回归通过，共 45 项浏览器检查。
- 覆盖长标题、宽表格、代码与长公式局部滚动、代码复制、Callout、搜索与历史、目录焦点、抽屉关闭、重试、阅读恢复与版本失效。
- 测试代码复制使用隔离页面中的剪贴板替身，不改变用户的系统剪贴板。所有文章与账号 API 使用隔离浏览器数据，不写真实 MySQL。
- 检查了桌面与手机截图。720px CSS 视口验证了 1440px 在 200% 缩放时对应的布局回流宽度，但没有执行浏览器原生 200% 缩放或完整辅助技术审计，不能将两者等同。
- 本地生产预览的三次冷加载中位 LCP：手机 868ms、桌面 1220ms，测得首页 CLS 为 0。条件与 `frontend-performance.md` 一致：4Mbps、40ms 延迟、3 倍 CPU 减速。结果不是线上速度保证。
- 首页解压后 JS 为 174,681B、CSS 为 27,822B；手机首屏图 162,060B、桌面 327,508B，均满足已有性能预算。CSS 相比上一批增加约 0.8KB，没有重新引入后台或阅读器首屏依赖。
- 现有 VueUse 注释与阅读器较大 chunk 警告仍存在。没有执行 Docker 内 `nginx -t`、线上接口或 Cloudflare/FRP 链路检查，部署时另行验证。

测试截图与原始报告位于系统临时目录：`linge-reading-layout-qa/before`、`linge-reading-layout-qa/after`、`linge-stage-a-before`、`linge-discovery-qa`、`linge-stage-a-performance`。这些文件不加入 Git。

## 设计自查

以下仅评价本批改动，1 分为基本没有该问题，10 分为严重；不是对尚未实施的整站计划宣告验收。

| 自查项 | 分数 | 说明 |
| --- | ---: | --- |
| 技术感渐变 | 1 | 没有新增渐变 |
| 默认靛紫主色 | 1 | 沿用现有五色 token |
| 等权图标卡片阵 | 1 | 未增加卡片阵列 |
| 卡片左侧彩条 | 1 | 未增加装饰条；Callout 保留内容语义样式 |
| 无意义玻璃 | 2 | 移除站长入口玻璃；既有侧栏与背景的收敛仍在后续批次 |
| 超大数字占位 | 1 | 未增加数字展示 |
| 圆角图标顶标题 | 1 | 未增加图标底座 |
| 全居中构图 | 1 | 保留左对齐；仅居中约束阅读区域宽度 |
| 默认字体未设计 | 2 | 本批不改字体，后续 B 再细化标题角色 |
| 界面类型选错 | 1 | 公开入口与阅读优先，不扩大管理操作的视觉权重 |

## 复现检查

在 `goalbot-frontend` 执行：

```powershell
npm.cmd run test:unit
npm.cmd run build
```

使用现有 production preview，或在端口空闲时执行 `npm.cmd run preview -- --host 127.0.0.1 --strictPort`。在另一个 PowerShell 中设置实际可用的 Playwright 和 Chrome 路径后运行：

```powershell
$env:PLAYWRIGHT_MODULE='C:/Users/Y7000P/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'
$env:CHROME_PATH='C:/Program Files/Google/Chrome/Application/chrome.exe'
$env:QA_BASE_URL='http://127.0.0.1:4173'

foreach ($suite in @('tests/reading-layout.browser.cjs', 'tests/note-discovery.browser.cjs', 'tests/projects.browser.cjs', 'tests/lazy-ui.browser.cjs', 'tests/performance.browser.cjs')) {
  node $suite
  if ($LASTEXITCODE -ne 0) { throw "测试失败：$suite" }
}
```

布局测试的 `QA_LAYOUT_PHASE=before` 用于未修改版本的基线采集，不用于宣称新布局合格。性能检查必须使用生产构建，不能指向 Vite 开发服务器。

## 本地提交与推送

以下只暂存本批文件和前一轮计划文档，避免把截图、临时目录和不相关项目混入提交。执行前确认暂存区没有其他不希望提交的修改。

```powershell
Set-Location "C:\Users\Y7000P\Desktop\This is true assistant"
git status --short

$files = @(
  'CHANGELOG.md'
  'docs/frontend-improvement-plan.md'
  'docs/frontend-stage-a.md'
  'goalbot-frontend/src/components/PublicSiteHeader.vue'
  'goalbot-frontend/src/views/PublicBlog.vue'
  'goalbot-frontend/src/views/OfficialNotes.vue'
  'goalbot-frontend/tests/reading-layout.browser.cjs'
  'goalbot-frontend/tests/note-discovery.browser.cjs'
  'goalbot-frontend/tests/lazy-ui.browser.cjs'
)
git add -- $files
if ($LASTEXITCODE -ne 0) { throw '暂存失败，停止执行' }
git diff --cached --stat
git commit -m "ui: clarify public entry points and improve note reading layout"
if ($LASTEXITCODE -ne 0) { throw '提交失败，停止执行' }
git push origin main
if ($LASTEXITCODE -ne 0) { throw '推送失败，请勿继续部署' }
```

Windows 本地 Git 不使用 Linux 的 `sudo`。

## 服务器更新

推送成功后，在运行 Docker 的本地服务器执行，不是 VPS。与自动更新任务错开；原有后端必须保持运行，供 Nginx 检查解析 upstream。

```bash
(
  set -e
  cd /home/glg/goalbot/Study-assistant
  sudo -u glg git pull --ff-only origin main
  sudo docker compose build goalbot-frontend
  sudo docker compose run --rm --no-deps goalbot-frontend nginx -t
  sudo docker compose up -d --no-deps --force-recreate goalbot-frontend
  sudo docker compose ps goalbot-frontend
  curl -fsSI http://127.0.0.1:18080/notes
)
```

GitHub 域名解析或拉取失败时立即停止，不继续构建旧版本。保持 `.env`、数据库卷和 FRP 不变，不执行 SQL，不重建后端，不修改 VPS Nginx。最后通过域名检查首页主入口、笔记列表与一篇真实的已公开文章。

## 尚未实施

没有调整首页遮罩与精选项目，没有增加项目媒体，没有改写简历或奖项，没有重做时间线、关于页和工作台。批次 B 及之后仍需按计划逐批推进。
