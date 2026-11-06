# SevaDesk

White-label administration platform for temples and places of worship. One deployment serves many temples;
each gets its own address, name, logo, colours, greeting and vocabulary ("Priest" → "Archaka", "Granthi", "Pastor").

| Layer | Stack |
|---|---|
| API | Java 21, Spring Boot 3.5, Spring Modulith, Spring Security (JWT in httpOnly cookie), Flyway |
| Database | PostgreSQL 16 (+ pgvector for the future chat assistant), row-level security per temple |
| Web | Angular 21, PrimeNG 21, ngx-translate (runtime vocabulary + languages) |
| Hosting | One VM, Docker Compose, Caddy (automatic HTTPS incl. custom domains), Cloudflare R2 backups |

## Two modules, one address

| URL | What |
|---|---|
| `https://www.iskconsomerset.org/` | **Public website**: server-rendered pages (good for Google), same menu and page URLs as the old Wix site, live events, subscribe form |
| `https://www.iskconsomerset.org/admin` | **SevaDesk admin**: Angular app for staff (events, donations, contacts, dashboard) |
| `https://www.iskconsomerset.org/api` | API used by the admin |

An event saved as **Published** in the admin appears on the public website on the next page load; drafts and
cancelled events stay hidden. The website's **Subscribe** form creates a contact in SevaDesk with email consent.

The website's menu, contact details, home page text and page content are per temple, in
[`R__site_iskcon_somerset.sql`](api/src/main/resources/db/tenants/R__site_iskcon_somerset.sql). Edit it and restart:
Flyway re-applies it. The page text there is placeholder copy written for this project; replace it with your own.

## What works today

- **Multi-tenancy**: the temple is resolved from the host name (`radhagovinda.sevadesk.app`, or a custom domain).
  PostgreSQL row-level security isolates temples even if a query forgets a filter; the API connects as a
  non-owner role so RLS can't be bypassed.
- **White-label branding**: `GET /api/public/branding` → the web app recolours PrimeNG, swaps the title,
  greeting, logo and terms before first paint.
- **Sign-in**: email + password, tenant-bound JWT (a token from one temple is rejected at another).
- **Contacts**: list, search, add, edit (gotra, nakshatra, consent flags, custom fields).
- **Donations**: entry with funds and payment modes, gap-free receipt numbers per fiscal year
  (`RG/2026-27/00042`), void with reason (never delete), role checks.
- **Expenses API** (`/api/expenses`); the screen comes in the Finance phase.
- **Live dashboard**: today / month / expenses / net, by fund, and a live donation feed over Server-Sent Events.
- **Ops**: Dockerfiles, production compose, Caddy with on-demand TLS, encrypted weekly + nightly backups to R2,
  restore script, GitHub Actions CI/CD (test → images → staging on `main` → production on `v*` tags).

Events, priest services, guest house, life members and volunteers show a "planned" page with their build phase.

## Run it locally

Prerequisites: Java 21+, Maven, Node 24.15+ (or 22.22+), Docker.

```bash
docker compose up -d                      # PostgreSQL with the app role
cd api && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd web && npm install && npm start        # in another terminal
```

If port 5432 is already used by a local PostgreSQL, start the database with `DB_PORT=55432 docker compose up -d`
and run the API with `DB_URL=jdbc:postgresql://localhost:55432/sevadesk` in front of the `mvn` command.

Open **http://localhost:4200** for the ISKCON Somerset public website and **http://localhost:4200/admin** for
SevaDesk. The other demo temples are at **http://radhagovinda.localhost:4200** and **http://gurunanak.localhost:4200**.
Sign in with the demo admin from [`application-dev.yml`](api/src/main/resources/application-dev.yml).

API docs: http://localhost:8080/api/docs (OpenAPI at `/api/docs/openapi`).

### Tests

```bash
cd api && mvn verify           # unit + Spring Modulith boundary check + Testcontainers (needs Docker)
cd web && npx ng test --watch=false
```

`TenantIsolationTests` signs in to two temples against real PostgreSQL and proves one can't see the other's data.

## Project layout

```
api/                      Spring Boot modular monolith
  src/main/java/com/sevadesk/
    platform/             tenancy, RLS DataSource, branding, auth, security
    contacts/             people
    donors/               funds, donations, receipt numbers
    finance/              expenses
    reporting/            dashboard totals + SSE stream
    devdata/              demo data (dev profile only)
  src/main/resources/db/  Flyway migrations (db/dev = demo tenants)
web/                      Angular app (core/ services, features/ screens, public/i18n)
infra/caddy/Caddyfile     HTTPS, custom domains, reverse proxy
infra/db/init/            creates the non-owner app role on first start
infra/backup/             backup.sh, restore.sh, crontab, image
docker-compose.yml        local database
docker-compose.prod.yml   production / staging stack
.github/workflows/ci.yml  CI/CD
```

## Online donations (Stripe)

`/donate` offers one-time, weekly, monthly and yearly card gifts, the temple's sponsorship amounts, an "in honor of"
note, an optional fee cover, and campaign buttons (e.g. 1000 Fortunate Souls: $200/month for 25 months, which ends
automatically after the 25th payment). Options are set in the temple's site settings (`donate` section).

Donors pay on Stripe's own checkout page. Stripe then calls the webhook, and SevaDesk records each paid gift (and
each monthly instalment) as a donation in the right fund, with a receipt number and the donor as a contact. It shows
on the live dashboard immediately. Repeated webhooks never record a gift twice.

Setup, in the temple's own Stripe account:
1. **Developers → API keys**: copy the secret key (`sk_test_…` first; `sk_live_…` when ready) into `STRIPE_SECRET_KEY`.
2. **Developers → Webhooks → Add endpoint**: `https://www.iskconsomerset.org/api/public/stripe/webhook`, events
   `checkout.session.completed`, `checkout.session.async_payment_succeeded`, `invoice.paid`. Copy its signing
   secret into `STRIPE_WEBHOOK_SECRET`.
3. Restart. With test keys the page shows a "Test mode" note; pay with card `4242 4242 4242 4242`.

Locally, run the API with `SEVADESK_STRIPE_ACCOUNTS_ISKCONSOMERSET_SECRETKEY` and
`SEVADESK_STRIPE_ACCOUNTS_ISKCONSOMERSET_WEBHOOKSECRET` set, and forward webhooks with the Stripe CLI:

```bash
stripe listen --events checkout.session.completed,checkout.session.async_payment_succeeded,invoice.paid --forward-to localhost:8080/api/public/stripe/webhook
```

## First admin login in production

Set `BOOTSTRAP_TENANT_SLUG=iskconsomerset`, `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` (12+ characters)
in the server's `.env`. On start, the API creates that admin only if the temple has no users yet. Sign in at
`/admin`, then delete the password line from `.env`.

## Adding a temple

Until the onboarding wizard exists, insert a row into `tenant` (see `db/dev/V1000__demo_tenants.sql` for the
columns), then create its first admin user. Point `<slug>.sevadesk.app` (wildcard DNS) or the temple's own
domain at the server; Caddy fetches the certificate on the first visit after checking
`/api/public/domain-allowed`.

## Deploy (first time)

1. VM with Docker (4 vCPU / 8 GB is plenty for the first 20+ temples). Open ports 80 and 443 only.
2. DNS: point `iskconsomerset.org` and `www.iskconsomerset.org` (A records) at the VM. Caddy gets the HTTPS
   certificates automatically. With Cloudflare proxying, use SSL mode **Full (strict)**.
3. `git clone` to `/opt/sevadesk`, `cp .env.example .env`, fill it in, `chmod 600 .env`.
4. Backups: `age-keygen -o backup.agekey`; put the public key in `.env`, store `backup.agekey` **off the server**.
   Create the R2 bucket with lifecycle rules (weekly/ 84 days, nightly/ 7 days).
5. Push a tag `v0.1.0`; CI builds images and deploys after approval. Or by hand:
   `TAG=v0.1.0 docker compose -f docker-compose.prod.yml --env-file .env up -d`.
6. Monthly: restore the latest backup into staging with `restore.sh` and check the row counts.

## Next steps (from the plan)

- TOTP two-factor login for admin/treasurer, login rate limiting, password reset
- PDF receipts (80G / 501(c)(3) wording per tenant) emailed on save; payment gateway webhooks
- Expense and finance screens; Excel export
- Events + social publishing outbox (email, Facebook/Instagram, X)
- Priest services, life members, guest house, volunteers
- Chat assistant (pgvector + Claude) with WhatsApp hand-off; tenant onboarding wizard; subscription billing
