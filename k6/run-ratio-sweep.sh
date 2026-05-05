#!/usr/bin/env bash
# Три прогона с постоянными VU и смесиями POST/GET:
#   POST /api/viewers (CRUD), GET /api/cinema/films/max-viewers-summary (CRUD), см. k6/cinema-constant.js.
#
# Результаты:
#   RESULT_CPU=0.5|1.0  ->  k6/reports/summary-<mix>-vus-<N>-cpu-<tag>.json
#                        и копия в results/cpu-<tag>/ (для plot_k6_cpu_results.py).
#   без RESULT_CPU       ->  ...-vus-<N>-run-<timestamp>.json (в results/ не копируется).
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p k6/reports results/cpu-0.5 results/cpu-1.0

BASE_MAIN="${BASE_URL_MAIN:-http://localhost:8080}"
BASE_MAIN="${BASE_MAIN%/}"
BASE_ADD="${BASE_URL_ADDITIONAL:-http://localhost:8081}"
BASE_ADD="${BASE_ADD%/}"
TARGET_VUS="${TARGET_VUS:-30}"
DURATION="${DURATION:-90s}"
SUMMARY_LIMIT="${SUMMARY_LIMIT:-100}"
K6_ROUTE="${K6_ROUTE:-server-to-server}"
K6_THRESHOLDS_OFF="${K6_THRESHOLDS_OFF:-1}"

echo "=== sweep: MAIN=${BASE_MAIN} TARGET_VUS=${TARGET_VUS} SUMMARY_LIMIT=${SUMMARY_LIMIT} ==="

normalize_result_cpu() {
  local x="${1// /}"
  case "$x" in
    0.5) echo "0.5" ;;
    1.0|1) echo "1.0" ;;
    *) echo "" ;;
  esac
}

RESULT_CPU_LABEL=""
if [[ -n "${RESULT_CPU:-}" ]]; then
  RESULT_CPU_LABEL="$(normalize_result_cpu "${RESULT_CPU}")"
  if [[ -z "${RESULT_CPU_LABEL}" ]]; then
    echo "Ошибка: RESULT_CPU='${RESULT_CPU}' — ожидается 0.5 или 1.0 (или 1)" >&2
    exit 1
  fi
fi

FILE_STEM_SUFFIX="-vus-${TARGET_VUS}"
if [[ -n "${RESULT_CPU_LABEL}" ]]; then
  FILE_STEM_SUFFIX+="-cpu-${RESULT_CPU_LABEL}"
else
  RUN_STAMP="${RUN_STAMP:-$(date +%Y%m%d-%H%M%S)}"
  FILE_STEM_SUFFIX+="-run-${RUN_STAMP}"
fi

run_one() {
  local share="$1"
  local tag="$2"
  local sum="k6/reports/summary-${tag}${FILE_STEM_SUFFIX}.json"
  echo "=== POST_SHARE=$share ($tag) TARGET_VUS=$TARGET_VUS ==="
  k6 run \
    -e "SUMMARY_FILE=$sum" \
    -e "BASE_URL_MAIN=${BASE_MAIN}" \
    -e "BASE_URL_ADDITIONAL=${BASE_ADD}" \
    -e "TARGET_VUS=${TARGET_VUS}" \
    -e "POST_SHARE=${share}" \
    -e "DURATION=${DURATION}" \
    -e "SUMMARY_LIMIT=${SUMMARY_LIMIT}" \
    -e "K6_ROUTE=${K6_ROUTE}" \
    -e "K6_HTTP_TIMEOUT=${K6_HTTP_TIMEOUT:-120s}" \
    -e "K6_HTTP_FAIL_RATE_MAX=${K6_HTTP_FAIL_RATE_MAX:-0.35}" \
    -e "K6_THRESHOLDS_OFF=${K6_THRESHOLDS_OFF}" \
    k6/cinema-constant.js
}

run_one 0.05 "post05-get95"
run_one 0.50 "post50-get50"
run_one 0.95 "post95-get05"

echo "Готово. JSON в k6/reports/summary-*${FILE_STEM_SUFFIX}.json"

if [[ -n "${RESULT_CPU_LABEL}" ]]; then
  dest="${ROOT}/results/cpu-${RESULT_CPU_LABEL}"
  mkdir -p "$dest"
  shopt -s nullglob
  copies=( k6/reports/summary-*-vus-${TARGET_VUS}-cpu-${RESULT_CPU_LABEL}.json )
  if [[ ${#copies[@]} -eq 0 ]]; then
    echo "Ошибка: нет файлов k6/reports/summary-*-vus-${TARGET_VUS}-cpu-${RESULT_CPU_LABEL}.json" >&2
    exit 1
  fi
  rm -f "${dest}"/summary-*-vus-${TARGET_VUS}-cpu-${RESULT_CPU_LABEL}.json
  cp -v "${copies[@]}" "$dest/"
  echo "Скопировано в ${dest}/"

  if [[ "${AUTO_PLOT:-0}" == "1" ]]; then
    TARGET_VUS="${TARGET_VUS}" "${ROOT}/k6/plot-from-results.sh"
  fi
fi
