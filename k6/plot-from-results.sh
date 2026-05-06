#!/usr/bin/env bash
# Графики из каталога с подпапками cpu-* (после run-ratio-sweep.sh с RESULT_CPU).
#   После run-ratio-sweep.sh с RESULT_CPU копии summary лежат в k6/results/cpu-0.5 и cpu-1.0.
#   Дополнительно ищется каталог results/… (старый вариант в корне репо).
#
# LAB13 по умолчанию: легенда POST — kafka-proxy → Kafka. Для старых прогонов REST:
#   POST_LEGEND='POST /api/viewers (среднее, мс)' TITLE_TAG='POST viewers / GET summary' ./k6/plot-from-results.sh
#
#   ./k6/plot-from-results.sh
#   TARGET_VUS=30 ./k6/plot-from-results.sh
#   ./k6/plot-from-results.sh /path/to/results /path/to/out_png
# По умолчанию PNG -> k6/png_k6/
#   K6_PNG_PREFIX — префикс имён PNG (по умолчанию vs-cpu; для LAB13 см. lab9-hl11-run-all.sh)
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Каталог-родитель для cpu-0.5 / cpu-1.0: аргумент $1, или RESULTS_DIR, или авто (k6/results приоритетнее results).
_default_results_dir() {
  for base in "$ROOT/k6/results" "$ROOT/results"; do
    if [[ -d "$base" ]] && compgen -G "$base/cpu-*" >/dev/null 2>&1; then
      printf '%s' "$base"
      return 0
    fi
  done
  if [[ -d "$ROOT/k6/results" ]]; then
    printf '%s' "$ROOT/k6/results"
  else
    printf '%s' "$ROOT/results"
  fi
}

RESULTS_DIR="${1:-${RESULTS_DIR:-$(_default_results_dir)}}"
OUT_DIR="${2:-${K6_PNG_OUT_DIR:-$ROOT/k6/png_k6}}"
VUS="${TARGET_VUS:-30}"
PNG_PREFIX="${K6_PNG_PREFIX:-vs-cpu}"

# Легенды осей (LAB13: запись через прокси в Kafka; см. k6/cinema-constant.js K6_WRITE_MODE).
POST_LEGEND="${POST_LEGEND:-POST kafka-proxy /produce/viewer → Kafka (среднее, мс)}"
GET_LEGEND="${GET_LEGEND:-GET /api/cinema/films/max-viewers-summary (среднее, мс)}"
TITLE_TAG="${TITLE_TAG:-POST запись (Kafka) / GET summary}"

if [[ -x "$ROOT/.venv/bin/python3" ]]; then
  PY="$ROOT/.venv/bin/python3"
elif command -v python3 >/dev/null 2>&1; then
  PY="python3"
else
  echo "Нужен python3 или $ROOT/.venv/bin/python3" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
echo "=== plot: RESULTS=$RESULTS_DIR OUT=$OUT_DIR VUS=$VUS PNG_PREFIX=$PNG_PREFIX (python: $PY) ==="

exec "$PY" "$ROOT/k6/plot_k6_cpu_results.py" "$RESULTS_DIR" -o "$OUT_DIR" \
  --vus "$VUS" \
  --summary-prefix summary \
  --title-tag "$TITLE_TAG" \
  --png-prefix "$PNG_PREFIX" \
  --post-legend "$POST_LEGEND" \
  --get-legend "$GET_LEGEND"
