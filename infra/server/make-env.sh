#!/bin/sh
# Creates .env for production from .env.example, with fresh random passwords and JWT secret.
#   infra/server/make-env.sh <base-domain> <admin-email> [acme-email] > .env.production
# Test without a domain: use <server-ip-with-dashes>.sslip.io as the base domain, e.g. 3-145-10-20.sslip.io,
# and open https://iskconsomerset.3-145-10-20.sslip.io
set -eu
BASE_DOMAIN="${1:?base domain, e.g. 3-145-10-20.sslip.io or sevadesk.app}"
ADMIN_EMAIL="${2:?first admin email}"
ACME_EMAIL="${3:-$ADMIN_EMAIL}"
HERE="$(cd "$(dirname "$0")/../.." && pwd)"

rand() { openssl rand -base64 48 | tr -d '/+=\n' | cut -c1-"$1"; }

sed \
  -e "s|^BASE_DOMAINS=.*|BASE_DOMAINS=$BASE_DOMAIN|" \
  -e "s|^BOOTSTRAP_ADMIN_EMAIL=.*|BOOTSTRAP_ADMIN_EMAIL=$ADMIN_EMAIL|" \
  -e "s|^BOOTSTRAP_ADMIN_PASSWORD=.*|BOOTSTRAP_ADMIN_PASSWORD=$(rand 20)|" \
  -e "s|^ACME_EMAIL=.*|ACME_EMAIL=$ACME_EMAIL|" \
  -e "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=$(rand 32)|" \
  -e "s|^APP_DB_PASSWORD=.*|APP_DB_PASSWORD=$(rand 32)|" \
  -e "s|^JWT_SECRET=.*|JWT_SECRET=$(rand 64)|" \
  "$HERE/.env.example"
