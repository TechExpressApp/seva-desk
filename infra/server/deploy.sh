#!/bin/sh
# Copies the committed code to the server and (re)builds and starts the stack there.
#   infra/server/deploy.sh ubuntu@<server-ip> [path/to/.env.production]
# Only files committed to Git are sent (git archive), so local secrets never leave this machine.
# The .env file is uploaded only when given, and only if the server doesn't already have one.
set -eu
TARGET="${1:?ssh target, e.g. ubuntu@3.145.10.20}"
ENV_FILE="${2:-}"
KEY="${SSH_KEY:-$HOME/.ssh/id_ed25519}"
SSH="ssh -i $KEY -o StrictHostKeyChecking=accept-new"
cd "$(dirname "$0")/../.."

echo "Sending $(git rev-parse --short HEAD) to $TARGET ..."
git archive --format=tar HEAD | $SSH "$TARGET" 'cd /opt/sevadesk && tar -xf -'

if [ -n "$ENV_FILE" ]; then
  $SSH "$TARGET" 'test -e /opt/sevadesk/.env' \
    && echo ".env already on the server; not replaced." \
    || { $SSH "$TARGET" 'umask 077 && cat > /opt/sevadesk/.env' < "$ENV_FILE"; echo "Uploaded .env"; }
fi

$SSH "$TARGET" 'cd /opt/sevadesk \
  && docker compose -f docker-compose.prod.yml --env-file .env up -d --build \
  && docker image prune -f >/dev/null \
  && docker compose -f docker-compose.prod.yml ps'
