# GoalBot 本地服务器 + FRP + 海外 VPS + Cloudflare 部署

## 1. 方案结论与边界

这套方案技术上可行，也很适合个人使用或小规模受邀测试：业务、MySQL、Dify 和飞书长连接都运行在本地服务器，海外 VPS 只作为公网入口和 FRP 中继，Cloudflare 提供 DNS、HTTPS 边缘入口和基础防护。

需要特别说明：如果本地服务器位于中国大陆，GoalBot 的实际业务程序和数据仍在中国大陆，海外 VPS 只是反向代理。它不等同于把完整业务源站迁移到境外，也不能仅凭域名解析、Cloudflare 或海外跳板得出“不需要备案”的结论。本文提供的是安全远程访问架构，不是规避法定义务的方案。

如果目标是形成边界清晰的境外公网托管，应把直接面向公众的前端、后端和数据库部署在境外 VPS，本地服务器只承担不公开的 Dify、批处理、备份或开发环境。若必须把正式业务和数据保留在境内本地服务器，建议将站点设为私人服务，通过 Cloudflare Access 仅允许受邀账号进入，并根据运营主体、内容、用户范围以及服务商最新要求确认适用规则。不要接入要求备案的中国大陆 CDN、云服务器或 Cloudflare China Network。

这套链路不会让境内访问天然变快。用户到 Cloudflare、Cloudflare 到海外 VPS、VPS 到本地服务器都可能跨境，晚高峰延迟和丢包需要实测。对于个人助手，这通常可以接受；对于公开商业服务，需要额外考虑可用性和合规咨询。

## 2. 推荐架构

```text
浏览器 / 手机 / 飞书中的网页链接
    |
    | HTTPS 443
    v
Cloudflare 标准全球网络，业务域名为橙云
    |
    | 可选但推荐：Cloudflare Access，仅允许受邀账号
    |
    | HTTPS 443，SSL 模式为 Full (strict)
    v
海外 VPS Nginx + Cloudflare Origin Certificate
    |
    | VPS Docker 内网 TCP 18080
    v
海外 VPS frps
    ^
    | FRP TLS 控制连接 TCP 7000
    | 由本地服务器主动发起
    |
本地服务器 frpc
    |
    | 本地 Docker 网络 goalbot-frontend:80
    v
GoalBot 前端 Nginx -> Spring Boot -> MySQL / Dify
```

职责划分：

| 节点 | 运行内容 | 保存的敏感数据 |
| --- | --- | --- |
| Cloudflare | DNS、TLS 边缘入口、WAF/限流 | 不保存 GoalBot 数据库 |
| 海外 VPS | Nginx、frps、Origin Certificate | FRP token 和证书私钥 |
| 本地服务器 | GoalBot、MySQL、Dify、frpc、飞书长连接 | 全部业务数据和 API 密钥 |

VPS 不运行 MySQL、Spring Boot 或 Dify。FRP 的业务远端端口 `18080` 只暴露在 VPS 的 Docker 网络中，不映射到 VPS 公网。此模式应理解为“境外入口访问本地私人服务”，不是“完整境外托管”。

## 3. 域名与端口

下面以这些值为例：

```text
业务域名: assistant.example.com
FRP 控制域名: frp.example.com
海外 VPS 公网 IP: 203.0.113.10
FRP 控制端口: 7000
FRP 业务端口: 18080
```

Cloudflare DNS 配置：

| 类型 | 名称 | 内容 | 代理状态 |
| --- | --- | --- | --- |
| A | assistant | 海外 VPS 公网 IP | 已代理，橙云 |
| A | frp | 海外 VPS 公网 IP | 仅 DNS，灰云 |

`assistant.example.com` 是用户访问入口。`frp.example.com` 只供本地 `frpc` 连接 VPS 的 `7000/TCP`，必须为灰云；普通 Cloudflare 代理不会转发任意 TCP 端口。也可以不创建 `frp` 记录，直接把 VPS IP 写入本地 `FRP_SERVER_ADDR`。

公网防火墙只放行：

```text
TCP 22    SSH，优先限制为管理 IP
TCP 80    Cloudflare HTTP 回源和 HTTPS 跳转
TCP 443   Cloudflare HTTPS 回源
TCP 7000  本地 frpc 到 frps 的控制连接
```

不要放行：

```text
TCP 18080 FRP 业务端口，只在 VPS Docker 内网使用
TCP 3306  MySQL
TCP 8080  Spring Boot
```

本地路由器不需要端口转发，本地服务器也不需要公网入站端口。飞书长连接和 Dify 调用都是由本地主动向外建立连接。

## 4. VPS 选择

先按网络质量选择地区，再看价格：

- 中国香港通常延迟较低，但价格和带宽成本较高。
- 日本、韩国可作为延迟和价格的折中。
- 新加坡覆盖亚洲较好，但中国大陆部分线路的晚高峰表现可能波动。
- 美国或欧洲 VPS 可以工作，但不建议作为中国用户的第一选择。

网关本身资源需求不高，个人使用通常从 `1 vCPU / 1 GB RAM` 起步即可，关键是线路、带宽、流量额度和稳定性。购买前最好用服务商测试 IP，从本地宽带执行 `ping`、`mtr` 或 `traceroute`，并在晚高峰测试丢包和 RTT。

选择允许开放 `80/443/7000` 的普通 VPS。确认服务商的可接受使用政策允许反向代理和个人 Web 服务。

## 5. Cloudflare HTTPS

在 Cloudflare `SSL/TLS` 中配置：

```text
Encryption mode: Full (strict)
Always Use HTTPS: On
```

不要使用 `Flexible`。它会让 Cloudflare 到 VPS 之间退回 HTTP，破坏端到端 HTTPS。

进入下面的位置创建源站证书：

```text
Cloudflare Dashboard -> SSL/TLS -> Origin Server -> Create Certificate
```

证书主机名至少包含：

```text
assistant.example.com
```

在 VPS 网关目录保存为：

```text
certs/origin.pem
certs/origin.key
```

限制私钥权限：

```bash
chmod 600 certs/origin.key
```

Origin Certificate 仅用于 Cloudflare 到 VPS 的回源连接，普通浏览器不会直接信任它，因此业务域名应保持橙云。给 `/api/*` 配置绕过缓存规则，静态 `/assets/*` 可以保留 Cloudflare 缓存。

本地优先、仅供自己和少量受邀用户使用时，建议再配置 Cloudflare Access：

```text
Cloudflare Zero Trust -> Access -> Applications -> Add self-hosted application
Domain: assistant.example.com
Policy: Allow
Include: 指定邮箱或受邀用户组
```

可以使用邮箱一次性验证码或现有身份提供商。这样匿名访问在到达 VPS 之前就会被拦截，GoalBot 自身登录继续作为第二层身份校验。飞书长连接由本地服务器主动出站，不受 Access 影响。若将来要开放给不固定的公众用户，应重新评估是否移除 Access、部署边界和适用要求，而不是直接把私人入口改成公开入口。

## 6. 部署海外 VPS 网关

在 VPS 安装 Docker 和 Compose 插件，然后复制仓库中的网关模板：

```bash
sudo mkdir -p /opt/goalbot-gateway
sudo chown -R "$USER":"$USER" /opt/goalbot-gateway
cp -r deploy/frp/vps/. /opt/goalbot-gateway/
cd /opt/goalbot-gateway
cp .env.example .env
```

生成一个随机 FRP token：

```bash
openssl rand -hex 32
```

编辑 VPS 的 `.env`：

```dotenv
GOALBOT_DOMAIN=assistant.example.com
FRPS_IMAGE=snowdreamtech/frps:latest
FRP_BIND_PORT=7000
FRP_REMOTE_PORT=18080
FRP_AUTH_TOKEN=前面生成的随机长token
```

把 Origin Certificate 放入 `certs` 后启动：

```bash
docker compose config
docker compose up -d
docker compose ps
docker compose logs --tail=100 frps nginx
```

`frpc` 尚未连接时，访问业务域名出现 `502` 是预期现象，说明 Cloudflare 和 VPS Nginx 已到达，但 FRP 业务代理还没有注册。

模板默认使用 `latest` 便于首次启动。第一次打通后，应将本地 `FRPC_IMAGE` 和 VPS `FRPS_IMAGE` 固定为同一个已验证版本，避免以后自动升级导致协议或配置不兼容。

## 7. 部署本地 GoalBot

在本地服务器进入 GoalBot 仓库根目录：

```bash
cp .env.example .env
nano .env
```

至少配置：

```dotenv
HTTP_BIND_ADDRESS=127.0.0.1
HTTP_PORT=18080

MYSQL_USERNAME=goalbot
MYSQL_PASSWORD=强密码
MYSQL_ROOT_PASSWORD=另一个强密码

GOALBOT_BOOTSTRAP_ADMIN_USERNAME=local_user
GOALBOT_BOOTSTRAP_ADMIN_PASSWORD=管理员初始密码
GOALBOT_AUTH_SESSION_DAYS=30

FRPC_IMAGE=snowdreamtech/frpc:latest
FRP_SERVER_ADDR=frp.example.com
FRP_SERVER_PORT=7000
FRP_REMOTE_PORT=18080
FRP_AUTH_TOKEN=与VPS完全相同的随机长token
```

继续配置 Dify 和飞书：

```dotenv
DIFY_ENABLED=true
DIFY_API_URL=http://host.docker.internal:5001/v1
DIFY_API_KEY=你的Dify密钥
DIFY_WORKFLOW_API_URL=http://host.docker.internal:5001/v1/workflows/run
DIFY_WORKFLOW_API_KEY=你的Workflow密钥

FEISHU_APP_ID=cli_xxx
FEISHU_APP_SECRET=xxx
FEISHU_LONG_CONNECTION_ENABLED=true
```

容器中的 `localhost` 指向容器自身。Dify 若运行在本地服务器宿主机，使用 `host.docker.internal`；当前 Compose 已为 Linux 配置 `host-gateway`。

启动业务和 FRP profile：

```bash
docker compose --profile tunnel up -d --build
docker compose --profile tunnel ps
docker compose --profile tunnel logs --tail=100 goalbot-frpc
```

不用 FRP 的本地开发仍可执行：

```bash
docker compose up -d --build
```

## 8. 防火墙与加固

以 Ubuntu UFW 为例，先确保 SSH 可用再启用规则：

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 7000/tcp
sudo ufw enable
sudo ufw status verbose
```

进一步建议：

- SSH 使用密钥，确认密钥登录可用后再关闭密码登录。
- 如果本地宽带出口 IP 固定，将 VPS 的 `7000` 来源限制为该 IP。
- FRP 使用随机长 token、强制 TLS、相同固定版本，不启用公开管理面板。
- 部署稳定后，可将 VPS 的 `80/443` 来源限制为 Cloudflare 官方 IP 段，但需要持续同步 Cloudflare IP 清单。
- 在 Cloudflare 为 `/api/auth/login` 配置限流或 WAF 规则。
- 不要把真实 `.env`、FRP token、Origin 私钥提交到 Git。

## 9. 分层验收

### 9.1 本地业务

在本地服务器执行：

```bash
curl -I http://127.0.0.1:18080/
docker compose --profile tunnel ps
docker compose logs --tail=100 goalbot-backend
```

应得到前端响应，并确认 MySQL、后端、前端和 `goalbot-frpc` 都处于运行状态。

### 9.2 FRP 控制连接

本地服务器：

```bash
docker compose --profile tunnel logs --tail=100 goalbot-frpc
```

日志应显示登录 frps 成功以及 `goalbot-web` 代理启动成功。若反复重连，检查灰云 DNS、VPS 防火墙 `7000`、token 和 FRP 版本。

### 9.3 VPS 内部业务端口

VPS 执行：

```bash
cd /opt/goalbot-gateway
docker compose exec nginx wget -S -O- http://frps:18080/ 2>&1 | head
```

能看到 GoalBot HTML 响应，说明完整 FRP 隧道已打通。

### 9.4 VPS HTTPS 源站

在自己的电脑绕过 Cloudflare 测试 VPS Nginx：

```bash
curl -kI --resolve assistant.example.com:443:203.0.113.10 https://assistant.example.com/
```

这里使用 `-k`，因为 Cloudflare Origin Certificate 不是公共浏览器 CA 证书。

### 9.5 Cloudflare 公网入口

```bash
curl -I https://assistant.example.com/
```

然后打开：

```text
https://assistant.example.com/login
```

登录接口验收：

```bash
curl -X POST https://assistant.example.com/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"local_user","password":"管理员密码"}'
```

最后实际验证：登录、查询任务、创建任务、刷新 AI 建议、飞书长连接收发消息和 ICS 上传。

## 10. 常见故障

### Cloudflare 523

Cloudflare 找不到 VPS 源站。检查业务域名 A 记录、VPS 公网 IP、防火墙 `443` 和 Nginx 容器。

### Cloudflare 525 / 526

检查 Origin Certificate 文件、证书主机名、私钥是否匹配，以及 SSL 模式是否为 `Full (strict)`。

### Nginx 502

VPS Nginx 已工作，但 `frps:18080` 没有可用代理。检查本地 `frpc` 是否在线、token 是否一致、两端远端端口是否都是 `18080`。

### frpc login failed / reconnecting

检查 `frp.example.com` 是否为灰云、`7000` 是否开放、frpc/frps 是否同版本、token 是否有多余空格。

### 页面可打开但 `/api` 失败

FRP 已打通，问题位于本地。检查 `goalbot-frontend` 到 `goalbot-backend:8080` 的 Docker 网络和后端日志。

### Dify 调用失败

检查 Dify URL 是否能从后端容器访问。宿主机服务不能写容器内的 `localhost`，应使用 `host.docker.internal` 或将 Dify 加入同一 Docker 网络。

### 境内访问慢或偶发断流

先分别测量本地到 VPS、客户端到业务域名的延迟和丢包。Cloudflare 只能优化用户到边缘及部分回源路径，无法消除 VPS 到本地的跨境链路。必要时更换更合适线路的 VPS，而不是继续叠加代理层。

## 11. 可用性与数据安全

完整链路依赖 Cloudflare、VPS、本地宽带、本地供电和 FRP，任一环节中断都会影响访问。个人使用建议至少做到：

- 本地服务器设置 Docker 开机自启，所有容器使用 `restart: unless-stopped`。
- VPS 和本地都配置磁盘、容器存活和 HTTPS 可用性监控。
- 本地服务器使用 UPS，路由器和光猫也接入后备电源。
- 定期备份 `goalbot_mysql_data`，并把备份复制到另一块磁盘或可信对象存储。
- 定期执行恢复演练，确认备份不是只有文件而无法恢复。
- VPS 网关不保存业务数据库，重建 VPS 时只需恢复网关配置、证书和 DNS。

## 12. 与 Cloudflare Tunnel 的取舍

| 方案 | 优点 | 缺点 |
| --- | --- | --- |
| 海外 VPS + FRP + Cloudflare | 中继位置和日志可控，可承载其他服务，FRP 行为清晰 | 多维护一台 VPS，多一层网络和一个公网 `7000` 端口 |
| 本地 cloudflared + Cloudflare Tunnel | 不需要 VPS 和公网入站端口，配置更少 | 完全依赖 Cloudflare 隧道和路由，在中国大陆网络下的实际质量仍需测试 |

当前 GoalBot 已有 FRP 模板，且你明确有海外 VPS，因此推荐先采用 VPS + FRP。不要同时叠加 FRP 和 Cloudflare Tunnel，它不会提高稳定性，只会让排障更复杂。

## 13. 推荐上线顺序

1. 在本地用 `127.0.0.1:18080` 验证 GoalBot 和数据库。
2. 在 VPS 启动 `frps + Nginx`，放行 `7000/80/443`。
3. 启动本地 `frpc`，从 VPS Docker 内网验证 `frps:18080`。
4. 配置 Cloudflare Origin Certificate 和 `Full (strict)`。
5. 配置业务橙云域名与 FRP 灰云域名。
6. 完成登录、任务、AI、飞书和文件上传的端到端验收。
7. 固定 FRP 镜像版本，收紧防火墙，启用监控与数据库备份。

## 14. 更新顺序

更新 GoalBot 时只操作本地服务器：

```bash
git pull
docker compose --profile tunnel up -d --build
docker compose --profile tunnel logs --tail=100 goalbot-backend goalbot-frpc
```

更新 FRP 时先在 VPS 更新 `frps`，随后立即在本地更新 `frpc`，保证两端版本一致。更新 Nginx 或 Origin Certificate 只需要操作 VPS 网关。
