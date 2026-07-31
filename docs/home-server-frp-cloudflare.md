# Home Server + FRP + VPS + Cloudflare

The site runs on the private home server. The VPS provides the public Nginx entry and `frps`; Cloudflare provides DNS and edge TLS. This preserves the current deployment topology while the application itself is reduced to the public site and owner notebook.

## Request Path

```text
Browser
  -> Cloudflare HTTPS
  -> VPS Nginx
  -> VPS frps remote port 18080
  -> home-server frpc
  -> goalbot-frontend:80
  -> goalbot-backend:8080
  -> goalbot-mysql:3306
```

The names `goalbot-frontend`, `goalbot-backend`, `goalbot-mysql`, and `goalbot_mysql_data` are retained only for deployment and data-volume compatibility.

## Home Server Environment

Use a loopback bind when the site should be reachable only through FRP:

```dotenv
HTTP_BIND_ADDRESS=127.0.0.1
HTTP_PORT=18080

MYSQL_USERNAME=goalbot
MYSQL_PASSWORD=replace_with_a_strong_password
MYSQL_ROOT_PASSWORD=replace_with_another_strong_password

SITE_BOOTSTRAP_ADMIN_USERNAME=local_user
SITE_BOOTSTRAP_ADMIN_PASSWORD=replace_with_a_strong_owner_password
SITE_AUTH_SESSION_DAYS=30

FRPC_IMAGE=snowdreamtech/frpc:latest
FRP_SERVER_ADDR=your_vps_ip_or_dns_name
FRP_SERVER_PORT=7000
FRP_REMOTE_PORT=18080
FRP_AUTH_TOKEN=the_same_random_token_as_frps
```

Start the application and tunnel:

```bash
docker compose --profile tunnel up -d --build
docker compose --profile tunnel ps
docker compose --profile tunnel logs --tail=100 goalbot-frpc
```

`HTTP_BIND_ADDRESS=127.0.0.1` blocks direct LAN access to port `18080`, but it does not block the Compose `frpc` service because that service reaches `goalbot-frontend:80` through the internal Docker network.

## VPS

Keep the existing `frps` system service and Nginx configuration. The Nginx upstream should be the FRP remote port:

```nginx
location / {
    proxy_pass http://127.0.0.1:18080;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

The VPS firewall normally needs `80/tcp`, `443/tcp`, and the configured FRP control port such as `7000/tcp`. Do not expose MySQL `3306`, Spring Boot `8080`, or FRP remote port `18080` publicly.

Use Cloudflare `Full (strict)` TLS with a valid origin certificate for the configured domain. Do not use `Flexible` mode.

## Update Procedure

On the home server, from `/home/glg/goalbot/Study-assistant`:

```bash
mkdir -p backups
docker compose exec -T goalbot-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers goalbot' > "backups/site-before-update-$(date +%Y%m%d-%H%M%S).sql"
git pull --ff-only
docker compose exec -T goalbot-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < goalbot-backend/sql/init.sql
docker compose exec -T goalbot-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" goalbot' < goalbot-backend/sql/note_knowledge_base.sql
docker compose up -d --build goalbot-backend goalbot-frontend
```

The update does not require changes to the VPS Nginx, Cloudflare DNS, certificates, `frps`, `frpc`, or the MySQL volume.

## Verification

Home server:

```bash
curl -I http://127.0.0.1:18080/
curl -fsS 'http://127.0.0.1:18080/api/public/notes?limit=1'
docker compose --profile tunnel ps
docker compose logs --tail=100 goalbot-backend goalbot-frontend
```

Public path:

```bash
curl -I https://linge.xin/
curl -I https://linge.xin/notes
curl -I https://linge.xin/login
curl -fsS 'https://linge.xin/api/public/notes?limit=1'
```

An empty note list is valid. A JSON response with `code: 0` confirms that the `note` table exists and the public API is healthy.

This architecture is a technical reverse-proxy arrangement, not legal advice. Confirm applicable hosting, content, and filing requirements for the actual service location and audience.
