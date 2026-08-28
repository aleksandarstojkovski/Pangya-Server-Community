#!/usr/bin/env bash
# Local CI: Gradle tests + docker compose health for the current slice.
# S3: postgres/redis healthy + protocol/network/db + Auth/Login/Game fake-client tests
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "== docker compose postgres+redis =="
docker compose up -d postgres redis
echo "waiting for postgres/redis health..."
for i in $(seq 1 60); do
  pg=$(docker inspect --format='{{.State.Health.Status}}' "$(docker compose ps -q postgres)" 2>/dev/null || echo starting)
  rd=$(docker inspect --format='{{.State.Health.Status}}' "$(docker compose ps -q redis)" 2>/dev/null || echo starting)
  echo "  postgres=$pg redis=$rd"
  if [[ "$pg" == "healthy" && "$rd" == "healthy" ]]; then
    break
  fi
  if [[ "$i" -eq 60 ]]; then
    echo "timeout waiting for postgres/redis" >&2
    docker compose ps
    exit 1
  fi
  sleep 2
done

echo "== gradle test (S0–S3 modules) =="
./gradlew --no-daemon \
  :core-protocol:test \
  :core-network:test \
  :core-db:test \
  :server-login:test \
  :server-auth:test \
  :server-game:test

echo "== docker compose ps =="
docker compose ps
echo "verify.sh OK"
