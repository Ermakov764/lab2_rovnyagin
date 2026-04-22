#!/usr/bin/env bash
# LAB4: серия прогонов с удвоением целевых VU (10, 20, 40, 80, 160), экспорт summary JSON.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORTS="${SCRIPT_DIR}/reports"
mkdir -p "${REPORTS}"

export BASE_URL="${BASE_URL:-http://localhost:8080}"
export FILM_ID="${FILM_ID:-1}"
export POST_SHARE="${POST_SHARE:-0.5}"

run_k6_native() {
  local v="$1"
  echo "=== TARGET_VUS=${v} (native k6) ==="
  TARGET_VUS="${v}" k6 run --summary-export "${REPORTS}/summary-vus-${v}.json" "${SCRIPT_DIR}/cinema-mixed.js"
}

run_k6_docker() {
  local v="$1"
  echo "=== TARGET_VUS=${v} (Docker grafana/k6) ==="
  local extra=()
  if [[ "$(uname -s)" == "Linux" ]]; then
    extra+=(--add-host=host.docker.internal:host-gateway)
  fi
  docker run --rm "${extra[@]}" \
    -e BASE_URL="${BASE_URL}" \
    -e FILM_ID="${FILM_ID}" \
    -e POST_SHARE="${POST_SHARE}" \
    -e TARGET_VUS="${v}" \
    -v "${SCRIPT_DIR}:/scripts" \
    grafana/k6 run \
    --summary-export "/scripts/reports/summary-vus-${v}.json" \
    "/scripts/cinema-mixed.js"
}

if [[ "${USE_DOCKER_K6:-0}" == "1" ]]; then
  for v in 10 20 40 80 160; do
    run_k6_docker "${v}"
  done
elif command -v k6 &>/dev/null; then
  for v in 10 20 40 80 160; do
    run_k6_native "${v}"
  done
else
  echo "k6 не найден в PATH. Варианты:"
  echo "  1) Установить: https://k6.io/docs/get-started/installation/"
  echo "  2) Запустить с Docker: USE_DOCKER_K6=1 ./k6/run-sweep.sh"
  echo "     (на Linux для доступа к API на хосте задайте BASE_URL=http://host.docker.internal:8080 при необходимости)"
  exit 1
fi

echo "Готово. JSON: ${REPORTS}/summary-vus-*.json"
echo "График: python3 ${SCRIPT_DIR}/plot_avg_vs_vus.py ${REPORTS}"
