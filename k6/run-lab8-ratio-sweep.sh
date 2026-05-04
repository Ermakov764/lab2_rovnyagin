#!/usr/bin/env bash
# LAB 8: три прогона с постоянными VU и смесиями POST/GET:
#   POST /api/viewers (CRUD), GET /api/cinema/films/max-viewers-summary (CRUD), см. k6/cinema-lab8-constant.js.
#
# Результаты:
#   RESULT_CPU=0.5|1.0  →  k6/reports/lab8-summary-<mix>-vus-<N>-cpu-<метка>.json
#                        и копия в results/cpu-<метка>/ (для plot_k6_cpu_results.py).
#   без RESULT_CPU       →  …-vus-<N>-run-<timestamp>.json (не затирает прошлые прогоны;
#                        в results/ не копируется — положите руками или повторите с RESULT_CPU).
#
# Графики после двух (и более) папок results/cpu-*:
#   LAB8_AUTO_PLOT=1 RESULT_CPU=… ./k6/run-lab8-ratio-sweep.sh
#   или коротко: ./k6/plot-lab8-from-results.sh   (TARGET_VUS по умолчанию 30, как в sweep)
#   или вручную: python3 k6/plot_k6_cpu_results.py . -o k6/png_k6 --vus <N> --summary-prefix lab8-summary …
#
# Очистка накопленных k6-зрителей (и билетов) в БД — перед/после прогона:
#   Шаблон с включённой очисткой: source k6/env.sh
#   LAB8_CLEAR_VIEWERS_BEFORE=1 — TRUNCATE viewers+tickets, фильмы не трогаем
#     (то же, что: python3 tools/seed_rest_data.py --base-url \"$BASE_URL_MAIN\" --endpoint viewers --clear)
#   LAB8_CLEAR_VIEWERS_AFTER=1 — то же после трёх прогонов
#   LAB8_CLEAR_USE_PYTHON=0 — только curl (если на машине k6 нет Python/зависимостей seed)
#
# Слабый CPU (0.5): см. cinema-lab8-constant.js:
#   K6_HTTP_TIMEOUT (по умолчанию 120s), K6_HTTP_FAIL_RATE_MAX (0…1, по умолчанию 0.35),
#   K6_THRESHOLDS_OFF=1 — не падать по порогу (смотри http_req_failed в JSON отчёте).
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
# Без source k6/env.sh порог http_req_failed часто «краснеет» на 0.5 CPU; по умолчанию отключаем (см. JSON).
# Строгий режим: K6_THRESHOLDS_OFF=0 ./k6/run-lab8-ratio-sweep.sh
K6_THRESHOLDS_OFF="${K6_THRESHOLDS_OFF:-1}"

echo "=== LAB8 sweep: MAIN=${BASE_MAIN} TARGET_VUS=${TARGET_VUS} SUMMARY_LIMIT=${SUMMARY_LIMIT} ==="

normalize_result_cpu() {
  local x="${1// /}"
  case "$x" in
    0.5) echo "0.5" ;;
    1.0|1) echo "1.0" ;;
    *) echo "" ;;
  esac
}

# Очистка viewers + tickets по API (как tools/seed_rest_data.py --endpoint viewers --clear).
lab8_clear_viewers_rest() {
  if [[ "${LAB8_CLEAR_USE_PYTHON:-1}" == "1" ]] && command -v python3 >/dev/null 2>&1 && [[ -f "${ROOT}/tools/seed_rest_data.py" ]]; then
    echo "=== Очистка БД: python3 tools/seed_rest_data.py --endpoint viewers --clear ==="
    python3 "${ROOT}/tools/seed_rest_data.py" --base-url "${BASE_MAIN}" --endpoint viewers --clear
    return
  fi
  local url="${BASE_MAIN}/api/admin/clear/viewers"
  echo "=== Очистка БД: curl -X DELETE ${url} ==="
  local code
  code="$(curl -sS -o /dev/null -w '%{http_code}' -X DELETE "${url}" --max-time 120)"
  if [[ "${code}" != "204" ]]; then
    echo "Ошибка: DELETE ${url} ожидался 204, получен ${code}" >&2
    exit 1
  fi
  echo "  OK (204)."
}

RESULT_CPU_LABEL=""
if [[ -n "${RESULT_CPU:-}" ]]; then
  RESULT_CPU_LABEL="$(normalize_result_cpu "${RESULT_CPU}")"
  if [[ -z "${RESULT_CPU_LABEL}" ]]; then
    echo "Ошибка: RESULT_CPU='${RESULT_CPU}' — для лаб. 8 ожидается 0.5 или 1.0 (или 1)" >&2
    exit 1
  fi
fi

FILE_STEM_SUFFIX="-vus-${TARGET_VUS}"
if [[ -n "${RESULT_CPU_LABEL}" ]]; then
  FILE_STEM_SUFFIX+="-cpu-${RESULT_CPU_LABEL}"
else
  LAB8_RUN_STAMP="${LAB8_RUN_STAMP:-$(date +%Y%m%d-%H%M%S)}"
  FILE_STEM_SUFFIX+="-run-${LAB8_RUN_STAMP}"
  echo "Подсказка: RESULT_CPU не задан — имена файлов …-run-${LAB8_RUN_STAMP}; для графиков задайте RESULT_CPU=0.5|1.0 (копирование в results/cpu-*/)." >&2
fi

if [[ "${LAB8_CLEAR_VIEWERS_BEFORE:-0}" == "1" ]]; then
  lab8_clear_viewers_rest
fi

run_one() {
  local share="$1"
  local tag="$2"
  local sum="k6/reports/lab8-summary-${tag}${FILE_STEM_SUFFIX}.json"
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
    -e "K6_HTTP_TIMEOUT=${K6_HTTP_TIMEOUT:-120s}" \
    -e "K6_HTTP_FAIL_RATE_MAX=${K6_HTTP_FAIL_RATE_MAX:-0.35}" \
    -e "K6_THRESHOLDS_OFF=${K6_THRESHOLDS_OFF}" \
    k6/cinema-lab8-constant.js
}

run_one 0.05 "post05-get95"
run_one 0.50 "post50-get50"
run_one 0.95 "post95-get05"

if [[ "${LAB8_CLEAR_VIEWERS_AFTER:-0}" == "1" ]]; then
  lab8_clear_viewers_rest
fi

echo "Готово. JSON в k6/reports/lab8-summary-*${FILE_STEM_SUFFIX}.json"

if [[ -n "${RESULT_CPU_LABEL}" ]]; then
  dest="${ROOT}/results/cpu-${RESULT_CPU_LABEL}"
  mkdir -p "$dest"
  shopt -s nullglob
  copies=( k6/reports/lab8-summary-*-vus-${TARGET_VUS}-cpu-${RESULT_CPU_LABEL}.json )
  if [[ ${#copies[@]} -eq 0 ]]; then
    echo "Ошибка: нет файлов k6/reports/lab8-summary-*-vus-${TARGET_VUS}-cpu-${RESULT_CPU_LABEL}.json" >&2
    exit 1
  fi
  rm -f "${dest}"/lab8-summary-*-vus-${TARGET_VUS}-cpu-${RESULT_CPU_LABEL}.json
  cp -v "${copies[@]}" "$dest/"
  echo "Скопировано в ${dest}/"

  if [[ "${LAB8_AUTO_PLOT:-0}" == "1" ]]; then
    TARGET_VUS="${TARGET_VUS}" "${ROOT}/k6/plot-lab8-from-results.sh"
  else
    echo "Графики: LAB8_AUTO_PLOT=1 при следующем прогоне или:"
    echo "  TARGET_VUS=${TARGET_VUS} ${ROOT}/k6/plot-lab8-from-results.sh"
  fi
fi
