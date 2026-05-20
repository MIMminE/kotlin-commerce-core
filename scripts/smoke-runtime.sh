#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-runtime/docker-compose.yml}"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-kotlin-commerce-runtime-smoke}"

cleanup() {
  docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" down -v --remove-orphans
}

trap cleanup EXIT

docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" up -d --build

for attempt in $(seq 1 90); do
  if curl -fsS http://localhost:8080/products >/dev/null; then
    echo "Runtime smoke test passed."
    exit 0
  fi
  sleep 5
done

docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" logs
echo "Runtime smoke test failed." >&2
exit 1
