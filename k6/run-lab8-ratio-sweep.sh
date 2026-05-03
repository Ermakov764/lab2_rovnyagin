#!/usr/bin/env bash
# LAB 8: три прогона с постоянными VU и смесиями POST/GET:
#   POST /api/viewers (CRUD), GET /api/cinema/films/max-viewers-summary (CRUD), см. k6/cinema-lab8-constant.js.
#
# Результаты: k6/reports/lab8-summary-*.json → при RESULT_CPU — k6/cpu-runs/cpu-*
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
CPU_RUNS="${K6_CPU_RUNS_ROOT:-${ROOT}/k6/cpu-runs}"
mkdir -p k6/reports "${CPU_RUNS}/cpu-0.5" "${CPU_RUNS}/cpu-1.0"

BASE_MAIN="${BASE_URL_MAIN:-http://localhost:8080}"
BASE_MAIN="${BASE_MAIN%/}"
BASE_ADD="${BASE_URL_ADDITIONAL:-http://localhost:8081}"
BASE_ADD="${BASE_ADD%/}"
TARGET_VUS="${TARGET_VUS:-30}"
DURATION="${DURATION:-90s}"
SUMMARY_LIMIT="${SUMMARY_LIMIT:-100}"
K6_ROUTE="${K6_ROUTE:-server-to-server}"

echo "=== LAB8 sweep: MAIN=${BASE_MAIN} TARGET_VUS=${TARGET_VUS} SUMMARY_LIMIT=${SUMMARY_LIMIT} ==="

normalize_result_cpu() {
  local x="${1// /}"
  case "$x" in
    0.5) echo "0.5" ;;
    1.0|1) echo "1.0" ;;
    *) echo "" ;;
  esac
}

run_one() {
  local share="$1"
  local tag="$2"
  local sum="k6/reports/lab8-summary-${tag}-vus-${TARGET_VUS}.json"
  echo "=== POST_SHARE=$share ($tag) TARGET_VUS=$TARGET_VUS ==="
  k6 run \
    -e "LAB8_SUMMARY_FILE=$sum" \
    -e "BASE_URL_MAIN=${BASE_MAIN}" \
    -e "BASE_URL_ADDITIONAL=${BASE_ADD}" \
    -e "TARGET_VUS=${TARGET_VUS}" \
    -e "POST_SHARE=${share}" \
    -e "DURATION=${DURATION}" \
    -e "SUMMARY_LIMIT=${SUMMARY_LIMIT}" \
    -e "K6_ROUTE=${K6_ROUTE}" \
    k6/cinema-lab8-constant.js
}

run_one 0.05 "post05-get95"
run_one 0.50 "post50-get50"
run_one 0.95 "post95-get05"

echo "Готово. JSON в k6/reports/lab8-summary-*.json"

if [[ -n "${RESULT_CPU:-}" ]]; then
  label="$(normalize_result_cpu "${RESULT_CPU}")"
  if [[ -z "$label" ]]; then
    echo "Ошибка: RESULT_CPU='${RESULT_CPU}' — для лаб. 8 ожидается 0.5 или 1.0 (или 1)" >&2
    exit 1
  fi
  dest="${CPU_RUNS}/cpu-${label}"
  mkdir -p "$dest"
  shopt -s nullglob
  rm -f "${dest}"/lab8-summary-*.json
  copies=(k6/reports/lab8-summary-*.json)
  if [[ ${#copies[@]} -eq 0 ]]; then
    echo "Ошибка: нет lab8-summary-*.json для копирования." >&2
    exit 1
  fi
  cp -v "${copies[@]}" "$dest/"
  echo "Скопировано в ${dest}/"

  if [[ "${LAB8_AUTO_PLOT:-0}" == "1" ]]; then
    plot_py="${ROOT}/k6/plot_lab6_from_results.py"
    mkdir -p "${ROOT}/png_k8"
    python3 "$plot_py" "${CPU_RUNS}" -o "${ROOT}/png_k8" \
      --vus "${TARGET_VUS}" \
      --summary-prefix lab8-summary \
      --title-tag "Лаб. 8 (POST viewers / GET summary)" \
      --png-prefix lab8-vs-cpu \
      --post-legend "POST /api/viewers (среднее, мс)" \
      --get-legend "GET /api/cinema/films/max-viewers-summary (среднее, мс)"
  fi
else
  echo "Подсказка: RESULT_CPU=0.5|1.0 — копирование в k6/cpu-runs/cpu-<метка>/"
fi
