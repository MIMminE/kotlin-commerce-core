#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-runtime/docker-compose.yml}"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-kotlin-commerce-runtime-smoke}"

cleanup() {
  docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" down -v --remove-orphans
}

trap cleanup EXIT

if ! docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" up -d --build; then
  docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" logs
  echo "Runtime compose startup failed." >&2
  exit 1
fi

wait_for_healthy() {
  local service="$1"

  for attempt in $(seq 1 150); do
    local container_id
    container_id="$(docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" ps -q "$service")"

    if [ -n "$container_id" ]; then
      local status
      status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$container_id")"

      if [ "$status" = "healthy" ]; then
        return 0
      fi

      if [ "$status" = "unhealthy" ]; then
        docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" logs "$service"
        echo "Service $service became unhealthy." >&2
        return 1
      fi
    fi

    sleep 5
  done

  docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" logs "$service"
  echo "Service $service did not become healthy." >&2
  return 1
}

for service in \
  zookeeper kafka redis \
  order-postgres inventory-postgres payment-postgres product-postgres \
  order-service inventory-service payment-service product-service; do
  wait_for_healthy "$service"
done

for attempt in $(seq 1 90); do
  if curl -fsS http://localhost:8080/actuator/health >/dev/null; then
    echo "Runtime smoke test passed."
    exit 0
  fi
  sleep 5
done

docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" logs
echo "Runtime smoke test failed." >&2
exit 1
