#!/usr/bin/env bash
# LAB4: серия прогонов k6 (10→160 VU), экспорт JSON, опционально график.
# По умолчанию: перед прогоном чистит k6/reports (*.json, avg_vs_vus.png),
# после прогонов ставит matplotlib при необходимости и строит avg_vs_vus.png.
#
# Переменные:
#   NO_CLEAN=1   — не удалять старые отчёты перед стартом
#   NO_PLOT=1    — не ставить matplotlib и не строить график
#   USE_DOCKER_K6=1 — k6 через grafana/k6
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORTS="${SCRIPT_DIR}/reports"
mkdir -p "${REPORTS}"

export BASE_URL="${BASE_URL:-http://localhost:8080}"
export FILM_ID="${FILM_ID:-1}"
export POST_SHARE="${POST_SHARE:-0.5}"

clean_reports() {
  if [[ "${NO_CLEAN:-0}" == "1" ]]; then
    echo "NO_CLEAN=1 — старые отчёты не удаляю."
    return 0
  fi
  echo "Очистка ${REPORTS} (summary-vus-*.json, avg_vs_vus.png)..."
  rm -f "${REPORTS}"/summary-vus-*.json "${REPORTS}"/avg_vs_vus.png 2>/dev/null || true
}

run_k6_native() {
  local v="$1"
  echo "=== TARGET_VUS=${v} (native k6) ==="
  TARGET_VUS="${v}" k6 run \
    --summary-export "${REPORTS}/summary-vus-${v}.json" \
    "${SCRIPT_DIR}/cinema-mixed.js"
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

ensure_matplotlib_and_plot() {
  if [[ "${NO_PLOT:-0}" == "1" ]]; then
    echo "NO_PLOT=1 — график не строю."
    echo "JSON: ${REPORTS}/summary-vus-*.json"
    return 0
  fi
  if ! python3 -c "import matplotlib" 2>/dev/null; then
    echo "matplotlib не найден — устанавливаю: python3 -m pip install --user \"matplotlib>=3.7\""
    python3 -m pip install --user "matplotlib>=3.7"
  fi
  python3 "${SCRIPT_DIR}/plot_avg_vs_vus.py" "${REPORTS}"
}

clean_reports

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
  echo "     (на Linux для API на хосте: BASE_URL=http://host.docker.internal:8080)"
  exit 1
fi

echo "Готово. JSON: ${REPORTS}/summary-vus-*.json"
ensure_matplotlib_and_plot
