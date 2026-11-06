#!/bin/sh
# Runs once, when the PostgreSQL data volume is first created.
# POSTGRES_USER owns the schema and runs Flyway migrations.
# APP_DB_USER is what the API connects as: not the owner and not a superuser, so row-level security always applies.
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
create role ${APP_DB_USER:-sevadesk_app} login password '${APP_DB_PASSWORD}';
SQL
