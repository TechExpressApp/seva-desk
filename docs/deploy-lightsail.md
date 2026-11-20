# Deploy on AWS Lightsail

One Lightsail instance runs everything: Caddy (HTTPS), the API and public website, PostgreSQL, and backups.

## 1. Create the instance (AWS console → Lightsail)

- **Platform** Linux/Unix, **Blueprint** OS only → **Ubuntu 24.04 LTS**
- **SSH key**: upload your public key (`~/.ssh/id_ed25519.pub`)
- **Plan**: 2 GB memory (1 GB is too small for Java + PostgreSQL)
- After it starts: **Networking** → attach a **static IP**, and in **IPv4 Firewall** add **HTTPS (TCP 443)**
  (SSH 22 and HTTP 80 are open by default)

## 2. Prepare the server (once)

```bash
ssh ubuntu@<ip> 'sudo sh -s' < infra/server/setup.sh
```

Installs Docker, adds 2 GB swap (needed to build the images on the server) and creates `/opt/sevadesk`.

## 3. Create the production settings (once)

```bash
# Test address that works without any DNS changes: replace dots in the IP with dashes
infra/server/make-env.sh 3-145-10-20.sslip.io you@example.org > .env.production
```

Generates random database passwords, JWT secret and a first-admin password (read it from `.env.production`).
Add Stripe / SMTP keys if needed. `.env.production` is ignored by Git; keep a copy somewhere safe.

## 4. Deploy (and every later update)

```bash
infra/server/deploy.sh ubuntu@<ip> .env.production   # first time uploads .env
infra/server/deploy.sh ubuntu@<ip>                   # later updates
```

Sends the committed code, builds the images on the server and starts them. The first build takes ~10 minutes.

Open `https://iskconsomerset.<ip-with-dashes>.sslip.io` (website) and `/admin` (sign in with the
`BOOTSTRAP_ADMIN_*` values). After signing in, delete `BOOTSTRAP_ADMIN_PASSWORD` from `/opt/sevadesk/.env`.

## 5. Real domain (when ready)

Point `iskconsomerset.org` and `www.iskconsomerset.org` (A records) at the static IP. Caddy gets the
certificates on the first visit. This replaces whatever site the domain shows today.

## Backups

Fill in the `BACKUP_*` and `RCLONE_CONFIG_R2_*` values in `/opt/sevadesk/.env`, add `COMPOSE_PROFILES=backup`,
and run `deploy.sh` again.

## Useful commands on the server

```bash
cd /opt/sevadesk
docker compose -f docker-compose.prod.yml logs -f api      # API logs
docker compose -f docker-compose.prod.yml ps               # status
docker stats                                                # memory use
```
