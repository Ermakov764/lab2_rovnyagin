#!/usr/bin/env bash
# Build CPU mix PNG charts into k6/png_k6/ from local k6/results/cpu-* (или results/cpu-*).
# Запуск с корня репозитория (или с ПК после sync-results-from-k6-vm.sh).
#
# Usage:
#   ./k6/plot-png.sh 40
#   TARGET_VUS=40 ./k6/plot-png.sh
#   RESULTS_DIR=k6/results ./k6/plot-png.sh 40
#
set -euo pipefail

# LAB13: по умолчанию легенда POST — kafka-proxy. Для REST-прогонов:
#   POST_LEGEND='POST /api/viewers (avg, ms)' TITLE_TAG='CPU mix (POST viewers / GET summary)' ./k6/plot-png.sh 30

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VUS="${1:-${TARGET_VUS:-}}"
RESULTS_DIR="${RESULTS_DIR:-${ROOT}/k6/results}"
PNG_DIR="${PNG_DIR:-${ROOT}/k6/png_k6}"
PLOT_PY="${ROOT}/k6/plot_k6_cpu_results.py"

POST_LEGEND="${POST_LEGEND:-POST kafka-proxy → Kafka (avg, ms)}"
GET_LEGEND="${GET_LEGEND:-GET /api/cinema/films/max-viewers-summary (avg, ms)}"
TITLE_TAG="${TITLE_TAG:-CPU mix report (POST Kafka proxy / GET summary)}"
PNG_PREFIX="${K6_PNG_PREFIX:-vs-cpu}"

[[ -f "$PLOT_PY" ]] || {
  echo "Нет $PLOT_PY — запускайте из корня lab2_rovnyagin." >&2
  exit 1
}
[[ -d "$RESULTS_DIR" ]] || {
  echo "Нет каталога: $RESULTS_DIR" >&2
  exit 1
}

python3 -c 'import matplotlib' 2>/dev/null || {
  echo 'Нужен matplotlib: pip install "matplotlib>=3.7"' >&2
  exit 1
}

cmd=(python3 "$PLOT_PY" "$RESULTS_DIR" -o "$PNG_DIR"
  --summary-prefix summary
  --title-tag "$TITLE_TAG"
  --png-prefix "$PNG_PREFIX"
  --post-legend "$POST_LEGEND"
  --get-legend "$GET_LEGEND")
[[ -n "$VUS" ]] && cmd+=(--vus "$VUS")

echo "==> ${cmd[*]}"
"${cmd[@]}"

echo "Готово: ${PNG_DIR}/${PNG_PREFIX}-mix-*.png"
