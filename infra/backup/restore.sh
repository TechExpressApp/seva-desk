#!/bin/sh
# Restore a backup into a database (use staging for the monthly restore drill, never prod by accident).
#   restore.sh weekly/sevadesk_weekly_2026-11-08_0130.dump.age  /keys/backup.agekey  sevadesk_restore
# The private age key is kept OFFLINE (password manager / printed), never on the server.
set -eu

OBJECT="$1"
KEY_FILE="$2"
TARGET_DB="${3:-sevadesk_restore}"

export PGHOST="${PGHOST:-db}" PGUSER="$POSTGRES_USER" PGPASSWORD="$POSTGRES_PASSWORD"

rclone copyto "r2:${BACKUP_BUCKET}/${OBJECT}" /tmp/restore.dump.age
createdb "$TARGET_DB" 2>/dev/null || true
age --decrypt --identity "$KEY_FILE" /tmp/restore.dump.age \
  | pg_restore --clean --if-exists --no-owner --dbname "$TARGET_DB"
rm -f /tmp/restore.dump.age

echo "Restored $OBJECT into $TARGET_DB. Row counts:"
psql -d "$TARGET_DB" -c "select 'tenants' t, count(*) from tenant union all select 'donations', count(*) from donation union all select 'contacts', count(*) from contact"
