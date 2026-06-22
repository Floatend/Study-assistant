# GoalBot Ubuntu Cloud Deployment

This guide deploys GoalBot with Docker Compose:

- `goalbot-frontend`: Nginx serving the Vue static build and proxying `/api`.
- `goalbot-backend`: Spring Boot API service.
- `goalbot-mysql`: MySQL 8.4 with persistent data volume.

## 1. Server Requirements

Recommended minimum:

- Ubuntu 22.04 or 24.04
- 2 CPU cores
- 2 GB RAM minimum, 4 GB recommended if Dify is also on the same server
- Ports `22` and `80` open in the cloud firewall
- Port `443` open later if you add HTTPS

## 2. Install Docker

On the Ubuntu server:

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
```

Optional, so you can run Docker without `sudo` after reconnecting SSH:

```bash
sudo usermod -aG docker $USER
```

## 3. Upload Or Pull Code

Put this project on the server, for example:

```bash
cd /opt
sudo mkdir -p goalbot
sudo chown -R $USER:$USER goalbot
cd /opt/goalbot
```

Then upload the project files here, or pull from your Git repository.

## 4. Create Environment File

```bash
cp .env.example .env
nano .env
```

Must change:

```dotenv
HTTP_BIND_ADDRESS=0.0.0.0
HTTP_PORT=80
MYSQL_PASSWORD=your_strong_goalbot_password
MYSQL_ROOT_PASSWORD=your_strong_root_password
GOALBOT_BOOTSTRAP_ADMIN_PASSWORD=your_strong_initial_admin_password
```

The repository `.env.example` defaults to `127.0.0.1:18080` for the private home-server + FRP topology. For this all-in-one public cloud guide, explicitly use `0.0.0.0:80` as shown above.

Fill these when the corresponding feature is enabled:

```dotenv
DIFY_API_URL=
DIFY_API_KEY=
DIFY_WORKFLOW_API_URL=
DIFY_WORKFLOW_API_KEY=
FEISHU_APP_ID=
FEISHU_APP_SECRET=
FEISHU_DEFAULT_CHAT_ID=
FEISHU_LONG_CONNECTION_ENABLED=true
```

If Dify runs on the same Ubuntu host but outside this compose stack, use `host.docker.internal`, for example:

```dotenv
DIFY_API_URL=http://host.docker.internal:5001/v1/chat-messages
DIFY_WORKFLOW_API_URL=http://host.docker.internal:5001/v1/workflows/run
```

## 5. Start Services

```bash
docker compose up -d --build
```

Check status:

```bash
docker compose ps
docker compose logs -f goalbot-backend
```

The first MySQL startup automatically runs:

```text
goalbot-backend/sql/init.sql
```

That creates the `goalbot` schema and all current tables.

## 6. Verify Deployment

Replace `SERVER_IP` with your server public IP:

```bash
curl http://SERVER_IP/
curl -X POST http://SERVER_IP/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"local_user","password":"your_strong_initial_admin_password"}'
```

In the browser:

```text
http://SERVER_IP/
```

Useful log commands:

```bash
docker compose logs -f goalbot-frontend
docker compose logs -f goalbot-backend
docker compose logs -f goalbot-mysql
```

## 7. Feishu Configuration

For long connection mode:

```dotenv
FEISHU_LONG_CONNECTION_ENABLED=true
FEISHU_APP_ID=cli_xxx
FEISHU_APP_SECRET=xxx
```

No public event callback URL is required for long connection mode.

If you switch back to URL event subscription later, configure Feishu event URL as:

```text
http://SERVER_IP/api/feishu/events
```

After adding HTTPS and a domain, use:

```text
https://your-domain.com/api/feishu/events
```

## 8. Existing Database Migration

If the MySQL volume already existed before a new SQL migration was added, Docker will not rerun `init.sql`.

Run a migration manually like this:

```bash
docker compose exec -T goalbot-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" goalbot' < goalbot-backend/sql/multi_user_auth.sql
```

For a brand-new cloud deployment, no manual migration is needed because `init.sql` already contains the latest schema.

## 9. Backup

Create a backup:

```bash
mkdir -p backups
docker compose exec -T goalbot-mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --databases goalbot' > backups/goalbot-$(date +%F-%H%M).sql
```

Restore a backup:

```bash
docker compose exec -T goalbot-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < backups/your-backup.sql
```

## 10. Update Deployment

After pulling new code:

```bash
docker compose up -d --build
docker compose logs -f goalbot-backend
```

If a schema migration was added, run the matching SQL file manually.

## 11. HTTPS Next Step

This compose stack exposes HTTP on port `80`.

For production, add HTTPS with one of these:

- Cloudflare proxy in front of the server
- Host-level Nginx + Certbot
- Nginx Proxy Manager
- Caddy

When HTTPS is ready, Feishu URL event mode should use the HTTPS URL.
