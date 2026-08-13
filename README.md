# linge.xin

`linge.xin` is a public personal website and learning-note archive. Visitors can browse the homepage, site introduction, and the notes explicitly published by the owner. The private area is limited to one administrator account for writing, importing, categorizing, previewing, and publishing Markdown notes.

The former GoalBot task, calendar, goal, check-in, AI, Dify, and Feishu features have been removed. The existing Compose service names, Java package name, database name, and MySQL volume name still contain `goalbot` so an existing deployment can reuse its current data volume without a destructive rename.

## Pages

```text
/           Public personal homepage
/journey    Public resume timeline
/notes      Public official-note archive
/about      Public site introduction
/login      Owner login
/notebook   Owner-only Markdown writing desk
```

Only notes with both `is_published = 1` and `is_official = 1` are visible on the public site.
The timeline combines projects, learning paths, courses, and certificates from `goalbot-frontend/src/data/timeline.ts`; edit that file to keep the page current.

## Project Layout

```text
.
|-- goalbot-frontend/   Vue 3 + Vite + TypeScript + Element Plus
|-- goalbot-backend/    Spring Boot + MyBatis Plus + MySQL
|-- deploy/frp/         Existing home-server and VPS tunnel configuration
|-- docker-compose.yml  MySQL, backend, frontend, and optional frpc
`-- docs/               Deployment guides
```

## Database

For a new database, execute the repeatable schema file:

```sql
SOURCE /absolute/path/to/goalbot-backend/sql/init.sql;
```

For an older deployment, run both repeatable migrations. They add missing note fields and indexes without deleting existing note data:

```sql
SOURCE /absolute/path/to/goalbot-backend/sql/init.sql;
SOURCE /absolute/path/to/goalbot-backend/sql/note_knowledge_base.sql;
```

The application now uses only `user`, `auth_session`, and `note`. Historical assistant tables are deliberately left untouched so they can be backed up before being archived or removed manually.

## Environment

Copy the example and provide real values outside Git:

```bash
cp .env.example .env
```

Required application settings:

```dotenv
MYSQL_USERNAME=goalbot
MYSQL_PASSWORD=replace_with_a_strong_password
MYSQL_ROOT_PASSWORD=replace_with_another_strong_password
SITE_BOOTSTRAP_ADMIN_USERNAME=local_user
SITE_BOOTSTRAP_ADMIN_PASSWORD=replace_with_a_strong_owner_password
SITE_AUTH_SESSION_DAYS=30
```

`SITE_BOOTSTRAP_ADMIN_PASSWORD` initializes a password only when the administrator has no usable password hash. It does not overwrite an existing password on every restart.

## Local Development

Start MySQL, then run the backend:

```bash
cd goalbot-backend
mvn spring-boot:run
```

Start the frontend in another terminal:

```bash
cd goalbot-frontend
npm install
npm run dev
```

Default development URLs:

```text
Frontend: http://localhost:5173/
Backend:  http://localhost:8080/
```

## HTTP API

```text
POST   /api/auth/login
GET    /api/auth/me
POST   /api/auth/logout
PUT    /api/auth/password

GET    /api/public/notes
GET    /api/public/notes/categories
GET    /api/public/notes/{id}

GET    /api/notes
GET    /api/notes/categories
GET    /api/notes/{id}
POST   /api/notes
POST   /api/notes/upload
PUT    /api/notes/{id}
DELETE /api/notes/{id}
```

All `/api/notes` management endpoints require an administrator bearer token. Public endpoints return official published notes only.

## Docker Deployment

Start the site on the private server:

```bash
docker compose up -d --build
```

To include the existing FRP client profile:

```bash
docker compose --profile tunnel up -d --build
```

Deployment guides:

- [Single Ubuntu host](docs/cloud-deploy.md)
- [Home server + FRP + VPS + Cloudflare](docs/home-server-frp-cloudflare.md)

Never commit `.env`, database dumps, FRP tokens, TLS private keys, or administrator passwords.
