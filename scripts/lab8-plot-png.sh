#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Зачем: собрать три графика lab8 (смеси POST viewers / GET summary vs CPU) в k6/png_k6/.
# Когда: после k6-прогонов, когда в ./results/cpu-0.5 и cpu-1.0 уже лежат lab8-summary-*.json.
# Где запускать: с локального ПК из корня репозитория lab2_rovnyagin.
# Типовой сценарий: ./scripts/sync-results-from-k6-vm.sh && ./scripts/lab8-plot-png.sh 40
# -----------------------------------------------------------------------------
# Лаб. 8: PNG в k6/png_k6/ из локального results/cpu-0.5 + cpu-1.0 (lab8-summary-*.json).
#
# Запуск из корня репозитория:
#   ./scripts/lab8-plot-png.sh 40
#   LAB8_PLOT_VUS=40 ./scripts/lab8-plot-png.sh
#
# Если в results/ остались lab6-папки cpu-1.5, cpu-2 — уберите их или задайте каталог:
#   LAB8_RESULTS_DIR=results-lab8 ./scripts/lab8-plot-png.sh 40
#
# Переменные:
#   LAB8_RESULTS_DIR  — каталог с подпапками cpu-* (по умолчанию ./results)
#   LAB8_PNG_DIR      — куда писать PNG (по умолчанию ./k6/png_k6)
#   LAB8_PLOT_VUS     — как --vus, если не задан первый аргумент
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

VUS="${1:-${LAB8_PLOT_VUS:-}}"
RESULTS_DIR="${LAB8_RESULTS_DIR:-${ROOT}/results}"
PNG_DIR="${LAB8_PNG_DIR:-${ROOT}/k6/png_k6}"
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
  --summary-prefix lab8-summary
  --title-tag "Лаб. 8 (POST viewers / GET summary)"
  --png-prefix lab8-vs-cpu
  --post-legend "POST /api/viewers (среднее, мс)"
  --get-legend "GET /api/cinema/films/max-viewers-summary (среднее, мс)")
[[ -n "$VUS" ]] && cmd+=(--vus "$VUS")

echo "==> ${cmd[*]}"
"${cmd[@]}"

echo "Готово: ${PNG_DIR}/lab8-vs-cpu-mix-*.png"
