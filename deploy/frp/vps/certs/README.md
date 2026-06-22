Place the Cloudflare Origin Certificate files in this directory on the overseas VPS:

```text
origin.pem  Cloudflare Origin Certificate
origin.key  Matching private key
```

Never commit the real certificate private key. On Linux, restrict it before starting Nginx:

```bash
chmod 600 certs/origin.key
```
