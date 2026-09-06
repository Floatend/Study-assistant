# 笔记查找与续读

## 使用流程

- `/notes` 展示分页笔记列表，每页 12 篇。可以按分类筛选，搜索标题或正文，或输入页码跳转。
- 搜索结果展示经过清理的命中片段。关键词只作为文字匹配，`%`、`_` 等字符不作为数据库通配符，标题与摘要不会被当作 HTML 执行。
- 选择文章后进入阅读页，手机上仍可从底部打开笔记或目录抽屉。
- “返回列表”保留搜索词、分类与页码；上下篇可以跨页继续阅读。浏览器前进、后退保留同样的上下文。
- 阅读后再次打开同一篇文章，会出现“继续阅读”入口。默认仍从开头显示，点击后才恢复上次位置。
- 正文末尾显示最多四篇同分类的公开文章。不填充其他分类或未公开内容。

## 续读数据

仅在浏览器 `localStorage` 的 `linge-note-reading-v1` 下保存文章 ID、更新时间、阅读比例和保存时间，不保存正文，也不上传服务器。

- 最多 50 条，90 天过期。
- 文章更新时间改变后，旧位置失效。
- 开头不足 3% 或结尾达到 98% 的位置不保留。
- 正常切换文章或加载页面引起的自动滚动不算主动阅读。
- 存储不可用或数据损坏时，阅读照常进行，只是不提供续读。
- 清理浏览器站点数据会同时清理这些记录；不同设备不会同步。

## 新增公开接口

所有接口使用现有的 `Result<T>` 返回格式，只读已发布且标记为站长公开的文章。

### GET /api/public/notes/search

参数：`keyword`（最多 100 字符）、`category`（最多 64 字符）、`descendants`（默认 true）、`page`（默认 1）、`pageSize`（默认 12，范围 1–50）。

返回：`items`、`total`、`page`、`pageSize`。超出最后一页会返回最后一个有效页；空结果返回第 1 页。

每条结果仅含 `id`、`title`、`category`、`tags`、`excerpt`、`wordCount`、`updatedAt`，不传整篇正文或用户信息。查询在 MySQL 内执行，只有当前页文章会参与摘要提取。

标题、正文、原摘要、标签和分类均可匹配。排序固定为更新时间、创建时间、ID 倒序。当前是面向个人知识库的子串搜索，不是分词搜索引擎；数据量明显增大后再评估专用索引。

### GET /api/public/notes/{id}/related

返回最多四篇同分类文章，排除自身、草稿和非站长公开文章。请求未公开或不存在的文章 ID 会返回业务错误。

### GET /api/public/notes/{id}/navigation

接受 `keyword`、`category`、`descendants`，返回 `previous`、`next` 和当前文章在结果中的一基序号 `position`。文章不属于当前查询结果时，前后项为空、序号为 0。

旧的 `/api/public/notes` 列表接口及站长工作台接口继续保留。无需新增表或回填摘要；原有文章读取时会应用新的摘要规则。

## 本地验证

项目根目录运行后端测试（需要 Maven/JDK 17+）：

```bash
mvn -f goalbot-backend/pom.xml test
```

前端测试兼容项目现有的 Node 20 工具链：

```bash
cd goalbot-frontend
npm run test:unit
npm run build
```

后端测试只连接内存 H2 数据库，包含 137 篇公开文章和额外的权限边界记录，不使用本地或服务器的 MySQL。浏览器交互测试使用隔离响应，不创建真实文章。

## 部署

先在开发机提交并推送本次文件。然后在运行 Docker 的本地部署服务器执行，而不是在仅运行 FRP/Nginx 的海外 VPS 执行：

```bash
cd /home/glg/goalbot/Study-assistant
git pull --ff-only origin main
docker compose build goalbot-backend goalbot-frontend
docker compose up -d --no-deps --force-recreate goalbot-backend goalbot-frontend
docker compose ps goalbot-backend goalbot-frontend
docker compose logs --tail=60 goalbot-backend goalbot-frontend
```

不需要执行 SQL、重启 MySQL、修改环境变量或调整 FRP/Nginx。不要运行 `docker compose down -v`，它会删除数据库卷。

待后端启动完成后测试公开搜索接口（如果修改过 HTTP_PORT，请替换 18080）：

```bash
curl -fsS 'http://127.0.0.1:18080/api/public/notes/search?page=1&pageSize=12'
```

预期 `code` 为 0，`data` 有分页字段，`items` 中没有整篇 `content`。HTTP 200 本身不代表业务成功，还需检查 JSON 中的 `code`。

随后在网站检查：真实正文关键词搜索、父分类跨页筛选、上下篇跨页、返回列表与后退、读到中间刷新后继续阅读。生产 MySQL 和云端联通性需要在部署后单独验收。
