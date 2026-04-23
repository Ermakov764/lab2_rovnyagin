#!/usr/bin/env bash
# LAB6: три прогона с постоянными VU и разными POST_SHARE (5/95, 50/50, 95/5).
# Результаты: k6/reports/lab6-summary-*.json
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p k6/reports

BASE_URL="${BASE_URL:-http://localhost:8080}"
TARGET_VUS="${TARGET_VUS:-30}"
DURATION="${DURATION:-90s}"
FILM_ID="${FILM_ID:-1}"

run_one() {
  local share="$1"
  local tag="$2"
  echo "=== POST_SHARE=$share ($tag) TARGET_VUS=$TARGET_VUS DURATION=$DURATION ==="
  k6 run --summary-export "k6/reports/lab6-summary-${tag}-vus-${TARGET_VUS}.json" \
    -e "BASE_URL=$BASE_URL" \
    -e "TARGET_VUS=$TARGET_VUS" \
    -e "POST_SHARE=$share" \
    -e "DURATION=$DURATION" \
    -e "FILM_ID=$FILM_ID" \
    k6/cinema-lab6-constant.js
}

run_one 0.05 "post05-get95"
run_one 0.50 "post50-get50"
run_one 0.95 "post95-get05"

echo "Готово. JSON в k6/reports/lab6-summary-*.json"
