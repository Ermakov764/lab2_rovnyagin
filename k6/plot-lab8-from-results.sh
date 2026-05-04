#!/usr/bin/env bash
# Графики лаб. 8 из results/cpu-* (после run-lab8-ratio-sweep.sh с RESULT_CPU).
#
#   ./k6/plot-lab8-from-results.sh
#   TARGET_VUS=30 ./k6/plot-lab8-from-results.sh
#   ./k6/plot-lab8-from-results.sh /path/to/results /path/to/out_png
# По умолчанию PNG → k6/png_k6/
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

RESULTS_DIR="${1:-$ROOT/results}"
OUT_DIR="${2:-$ROOT/k6/png_k6}"
VUS="${TARGET_VUS:-30}"

if [[ -x "$ROOT/.venv/bin/python3" ]]; then
  PY="$ROOT/.venv/bin/python3"
elif command -v python3 >/dev/null 2>&1; then
  PY="python3"
else
  echo "Нужен python3 или $ROOT/.venv/bin/python3" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
echo "=== plot lab8: RESULTS=$RESULTS_DIR OUT=$OUT_DIR VUS=$VUS (python: $PY) ==="

exec "$PY" "$ROOT/k6/plot_k6_cpu_results.py" "$RESULTS_DIR" -o "$OUT_DIR" \
  --vus "$VUS" \
  --summary-prefix lab8-summary \
  --title-tag "Лаб. 8 (POST viewers / GET summary)" \
  --png-prefix lab8-vs-cpu \
  --post-legend "POST /api/viewers (среднее, мс)" \
  --get-legend "GET /api/cinema/films/max-viewers-summary (среднее, мс)"
