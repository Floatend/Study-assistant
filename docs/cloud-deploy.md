# Ubuntu Docker Deployment

This guide deploys the public personal site and its owner-only notebook on one Ubuntu host. The Compose service names remain `goalbot-*` for compatibility with existing deployments.

## 1. Prerequisites

- Ubuntu 22.04 or 24.04
- Docker Engine with the Compose plugin
- A checked-out repository
- Ports `80` and `443` available when this host is the public entry

## 2. Configure Secrets

From the repository root:

```bash
cp .env.example .env
nano .env
```

Set at least:

```dotenv
HTTP_BIND_ADDRESS=0.0.0.0
HTTP_PORT=80
MYSQL_USERNAME=goalbot
MYSQL_PASSWORD=replace_with_a_strong_password
MYSQL_ROOT_PASSWORD=replace_with_another_strong_password
SITE_BOOTSTRAP_ADMIN_USERNAME=local_user
SITE_BOOTSTRAP_ADMIN_PASSWORD=replace_with_a_strong_owner_password
SITE_AUTH_SESSION_DAYS=30
```

Do not commit `.env`.

## 3. Start a New Deployment

```bash
docker compose config
docker compose up -d --build
docker compose ps
```

The MySQL container runs `goalbot-backend/sql/init.sql` automatically only when its data volume is created for the first time.

## 4. Update an Existing Deployment

Back up the current database before pulling new code:

```bash
mkdir -p backups
docker compose exec -T goalbot-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers goalbot' > "backups/site-before-update-$(date +%Y%m%d-%H%M%S).sql"
```

Pull the update, make sure `.env` contains the `SITE_*` settings, and apply the repeatable schema files:

```bash
git pull --ff-only
docker compose exec -T goalbot-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < goalbot-backend/sql/init.sql
docker compose exec -T goalbot-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" goalbot' < goalbot-backend/sql/note_knowledge_base.sql
docker compose up -d --build goalbot-backend goalbot-frontend
```

The migration does not drop historical assistant tables. Archive or remove them only after verifying the backup.

## 5. Verify

```bash
docker compose ps
docker compose logs --tail=100 goalbot-backend goalbot-frontend
curl -fsS 'http://127.0.0.1/api/public/notes?limit=1'
curl -I http://127.0.0.1/
curl -I http://127.0.0.1/notes
curl -I http://127.0.0.1/login
```

Expected behavior:

- `/`, `/notes`, and `/login` return the frontend application.
- `/api/public/notes` returns a JSON result with `code: 0`, even when the note list is empty.
- `/api/notes` returns `401` without an administrator token.
- Removed task, goal, AI, and Feishu controllers are no longer present in the backend.

## 6. Roll Back

Keep the database backup and the previous Git commit hash. If a release fails, restore the previous code first. Restore the SQL dump only when the database itself was changed incorrectly; do not overwrite newer note data merely to roll back frontend code.
