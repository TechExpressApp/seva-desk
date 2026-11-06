#!/bin/sh
# Encrypted PostgreSQL backup to Cloudflare R2 (or any S3-compatible bucket).
#   backup.sh weekly   -> r2:$BACKUP_BUCKET/weekly/   (the required weekly backup, Sunday)
#   backup.sh nightly  -> r2:$BACKUP_BUCKET/nightly/  (recommended extra)
# Retention is set with R2 lifecycle rules: weekly 84 days, nightly 7 days.
# rclone reads its remote from RCLONE_CONFIG_R2_* environment variables (see .env.example).
set -eu

KIND="${1:-weekly}"
TS="$(date +%Y-%m-%d_%H%M)"
FILE="/tmp/sevadesk_${KIND}_${TS}.dump.age"

export PGHOST="${PGHOST:-db}" PGUSER="$POSTGRES_USER" PGPASSWORD="$POSTGRES_PASSWORD"

echo "[$(date)] starting $KIND backup"
pg_dump --format=custom --no-owner "$POSTGRES_DB" | age --recipient "$BACKUP_AGE_PUBLIC_KEY" > "$FILE"
SIZE="$(du -h "$FILE" | cut -f1)"

rclone copy "$FILE" "r2:${BACKUP_BUCKET}/${KIND}/" --s3-no-check-bucket
rm -f "$FILE"
echo "[$(date)] uploaded $KIND backup ($SIZE)"

# Dead-man's switch: alerts you if this ping stops arriving (healthchecks.io, Better Stack, ...)
if [ -n "${BACKUP_HEALTHCHECK_URL:-}" ]; then
  curl -fsS -m 10 "${BACKUP_HEALTHCHECK_URL}/${KIND}" > /dev/null || true
fi
