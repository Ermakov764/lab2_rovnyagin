#!/usr/bin/env bash
set -euo pipefail

# Lab9 helper for hl03 (service VM).
# Usage:
#   bash scripts/lab9-hl3.sh prepare
#   bash scripts/lab9-hl3.sh set-cpu 0.5
#   bash scripts/lab9-hl3.sh set-cpu 1.0
#   bash scripts/lab9-hl3.sh logs <stamp>
#   bash scripts/lab9-hl3.sh status
#
# Optional env:
#   REPO_DIR=~/lab2_rovnyagin
#   COMPOSE_FILE=docker-compose.hl12.yml
#   ENV_FILE=.env

REPO_DIR="${REPO_DIR:-$HOME/lab2_rovnyagin}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.hl12.yml}"
ENV_FILE="${ENV_FILE:-.env}"

cd "$REPO_DIR"

compose_up() {
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --force-recreate
}

set_cpu() {
  local cpu="$1"
  case "$cpu" in
    0.5|1.0|1) ;;
    *)
      echo "CPU must be 0.5 or 1.0 (or 1)"
      exit 1
      ;;
  esac
  [[ "$cpu" == "1" ]] && cpu="1.0"

  sed -i "s/^APP_CPU_LIMIT=.*/APP_CPU_LIMIT=${cpu}/" "$ENV_FILE"
  sed -i "s/^ADDITIONAL_CPU_LIMIT=.*/ADDITIONAL_CPU_LIMIT=${cpu}/" "$ENV_FILE"
  compose_up

  echo "CPU updated to ${cpu}"
  docker inspect lab8_crud_app --format 'crud nano cpus: {{.HostConfig.NanoCpus}}'
  docker inspect lab8_additional_app --format 'additional nano cpus: {{.HostConfig.NanoCpus}}'
}

prepare() {
  docker build -t lavrentiyermakov/lab2_rovnyagin:latest .
  docker build -f Dockerfile.additional-service -t lavrentiyermakov/lab2_additional:lab8-20260503 .
  compose_up
  status
}

status() {
  docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}"
  echo
  curl -sf "http://127.0.0.1:8080/api/cinema/ping" && echo " <- main ping ok"
  curl -sf "http://127.0.0.1:8080/api/observability" >/dev/null && echo "main observability ok"
  curl -sf "http://127.0.0.1:8081/api/observability" >/dev/null && echo "additional observability ok"
}

collect_logs() {
  local stamp="${1:-}"
  if [[ -z "$stamp" ]]; then
    echo "Usage: $0 logs <stamp>"
    exit 1
  fi
  mkdir -p "$REPO_DIR/logs"
  docker logs lab8_crud_app > "$REPO_DIR/logs/main-${stamp}.log" 2>&1
  docker logs lab8_additional_app > "$REPO_DIR/logs/additional-${stamp}.log" 2>&1
  echo "Saved:"
  echo "  $REPO_DIR/logs/main-${stamp}.log"
  echo "  $REPO_DIR/logs/additional-${stamp}.log"
}

cmd="${1:-help}"
case "$cmd" in
  prepare) prepare ;;
  set-cpu) set_cpu "${2:-}" ;;
  logs) collect_logs "${2:-}" ;;
  status) status ;;
  *)
    echo "Usage:"
    echo "  $0 prepare"
    echo "  $0 set-cpu <0.5|1.0>"
    echo "  $0 logs <stamp>"
    echo "  $0 status"
    ;;
esac
