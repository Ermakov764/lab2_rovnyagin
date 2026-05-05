#!/usr/bin/env bash
# Build CPU mix PNG charts into k6/png_k6/ from local results/cpu-*.
#
# Usage:
#   ./scripts/plot-png.sh 40
#   TARGET_VUS=40 ./scripts/plot-png.sh
#   RESULTS_DIR=results ./scripts/plot-png.sh 40
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VUS="${1:-${TARGET_VUS:-}}"
RESULTS_DIR="${RESULTS_DIR:-${ROOT}/results}"
PNG_DIR="${PNG_DIR:-${ROOT}/k6/png_k6}"
PLOT_PY="${ROOT}/k6/plot_k6_cpu_results.py"

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
  --title-tag "CPU mix report (POST viewers / GET summary)"
  --png-prefix vs-cpu
  --post-legend "POST /api/viewers (avg, ms)"
  --get-legend "GET /api/cinema/films/max-viewers-summary (avg, ms)")
[[ -n "$VUS" ]] && cmd+=(--vus "$VUS")

echo "==> ${cmd[*]}"
"${cmd[@]}"

echo "Готово: ${PNG_DIR}/vs-cpu-mix-*.png"
