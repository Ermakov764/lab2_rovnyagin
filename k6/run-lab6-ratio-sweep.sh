#!/usr/bin/env bash
# LAB6: три прогона с постоянными VU и разными POST_SHARE (5/95, 50/50, 95/5).
#
# С RESULT_CPU: JSON в стиле zil/k6 (одногруппницы):
#   k6/reports-lab6-pc/   при K6_ROUTE=pc-to-server  → pc_cpu10_mix50.json …
#   k6/reports-lab6-s2s/  при K6_ROUTE=server-to-server → s2s_cpu10_mix50.json …
#   cpu05|10|15|20 = 0.5 / 1.0 / 1.5 / 2.0 vCPU; mix05|50|95 = смеси.
#
# Без RESULT_CPU: как раньше — только k6/reports/lab6-summary-*.json (черновик без cpu в имени).
#
# Копия в results/cpu-* при RESULT_CPU — для plot_lab6_from_results.py (три отдельных PNG).
#
# Опционально LAB6_AUTO_PLOT=1 — plot_lab6_from_results.py (legacy).
# LAB6_AUTO_PLOT_PANELS=1 — после полной серии: python3 k6/plot_k6_reports.py --lab6 <папка>
#   (имеет смысл, когда в reports-lab6-* уже есть все 4×3 JSON).
#
# K6_NO_THRESHOLDS=1 — передать k6 --no-thresholds (не прерывать sweep из-за http_req_failed;
#   при 400 VU и узком CPU часть 5xx/таймаутов может дать rate>20%). Для отчёта всё равно
#   смотрите summary и при необходимости снизьте TARGET_VUS.
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p k6/reports

mkdir -p \
  "${ROOT}/results/cpu-0.5" \
  "${ROOT}/results/cpu-1.0" \
  "${ROOT}/results/cpu-1.5" \
  "${ROOT}/results/cpu-2"

BASE_URL="${BASE_URL:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"
TARGET_VUS="${TARGET_VUS:-30}"
DURATION="${DURATION:-90s}"
FILM_ID="${FILM_ID:-1}"
K6_ROUTE="${K6_ROUTE:-pc-to-server}"

lab6_route_dir_and_prefix() {
  case "$(echo "${K6_ROUTE}" | tr '[:upper:]' '[:lower:]' | tr -d ' _-')" in
    pctoserver|frompc)
      echo "reports-lab6-pc|pc"
      ;;
    servertoserver|s2s)
      echo "reports-lab6-s2s|s2s"
      ;;
    *)
      echo "reports-lab6-pc|pc"
      ;;
  esac
}

IFS='|' read -r LAB6_REPORT_SUBDIR LAB6_FILE_PREFIX <<< "$(lab6_route_dir_and_prefix)"
LAB6_REPORTS_DIR="${ROOT}/k6/${LAB6_REPORT_SUBDIR}"
mkdir -p "${LAB6_REPORTS_DIR}"

echo "=== LAB6 sweep: BASE_URL=${BASE_URL} TARGET_VUS=${TARGET_VUS} DURATION=${DURATION} FILM_ID=${FILM_ID} K6_ROUTE=${K6_ROUTE} ==="
echo "    отчёты LAB6: ${LAB6_REPORTS_DIR}/ (${LAB6_FILE_PREFIX}_cpu*_mix*.json при заданном RESULT_CPU)"

normalize_result_cpu() {
  local x="${1// /}"
  case "$x" in
    0.5) echo "0.5" ;;
    1.0|1) echo "1.0" ;;
    1.5) echo "1.5" ;;
    2.0|2) echo "2" ;;
    *) echo "" ;;
  esac
}

cpu_tenths() {
  case "$1" in
    0.5) echo "05" ;;
    1.0) echo "10" ;;
    1.5) echo "15" ;;
    2) echo "20" ;;
    *) echo "" ;;
  esac
}

run_one() {
  local share="$1"
  local legacy_tag="$2"
  local mix_code="$3"
  local sum=""
  if [[ -n "${RESULT_CPU:-}" ]]; then
    local label tent
    label="$(normalize_result_cpu "${RESULT_CPU}")"
    tent="$(cpu_tenths "${label}")"
    if [[ -z "$tent" ]]; then
      echo "Ошибка: RESULT_CPU='${RESULT_CPU}'" >&2
      exit 1
    fi
    sum="${LAB6_REPORTS_DIR}/${LAB6_FILE_PREFIX}_cpu${tent}_mix${mix_code}.json"
  else
    sum="k6/reports/lab6-summary-${legacy_tag}-vus-${TARGET_VUS}.json"
  fi
  echo "=== POST_SHARE=$share (mix ${mix_code}) TARGET_VUS=${TARGET_VUS} → ${sum##*/} ==="
  local k6_args=()
  if [[ "${K6_NO_THRESHOLDS:-0}" == "1" ]]; then
    k6_args+=(--no-thresholds)
  fi
  k6 run "${k6_args[@]}" \
    -e "LAB6_SUMMARY_FILE=$sum" \
    -e "BASE_URL=$BASE_URL" \
    -e "TARGET_VUS=$TARGET_VUS" \
    -e "POST_SHARE=$share" \
    -e "DURATION=$DURATION" \
    -e "FILM_ID=$FILM_ID" \
    -e "K6_ROUTE=$K6_ROUTE" \
    k6/cinema-lab6-constant.js
}

run_one 0.05 "post05-get95" "05"
run_one 0.50 "post50-get50" "50"
run_one 0.95 "post95-get05" "95"

if [[ -n "${RESULT_CPU:-}" ]]; then
  echo "Готово. JSON: ${LAB6_REPORTS_DIR}/${LAB6_FILE_PREFIX}_cpu*_mix*.json"
else
  echo "Готово. JSON: k6/reports/lab6-summary-*.json"
fi

if [[ -n "${RESULT_CPU:-}" ]]; then
  label="$(normalize_result_cpu "${RESULT_CPU}")"
  if [[ -z "$label" ]]; then
    echo "Ошибка: RESULT_CPU='${RESULT_CPU}' — ожидается одно из: 0.5, 1.0, 1.5, 2 (или 2.0)" >&2
    exit 1
  fi
  dest="${ROOT}/results/cpu-${label}"
  mkdir -p "$dest"
  tent="$(cpu_tenths "${label}")"
  mix05="${LAB6_REPORTS_DIR}/${LAB6_FILE_PREFIX}_cpu${tent}_mix05.json"
  mix50="${LAB6_REPORTS_DIR}/${LAB6_FILE_PREFIX}_cpu${tent}_mix50.json"
  mix95="${LAB6_REPORTS_DIR}/${LAB6_FILE_PREFIX}_cpu${tent}_mix95.json"
  for f in "$mix05" "$mix50" "$mix95"; do
    if [[ ! -f "$f" ]]; then
      echo "Ошибка: нет файла $f" >&2
      exit 1
    fi
  done
  shopt -s nullglob
  rm -f "${dest}"/lab6-summary-*.json
  # Алиасы имён для plot_lab6_from_results.py (три отдельных PNG)
  cp -f "$mix05" "${dest}/lab6-summary-post05-get95-vus-${TARGET_VUS}.json"
  cp -f "$mix50" "${dest}/lab6-summary-post50-get50-vus-${TARGET_VUS}.json"
  cp -f "$mix95" "${dest}/lab6-summary-post95-get05-vus-${TARGET_VUS}.json"
  echo "Копия для legacy-plot: ${dest}/lab6-summary-*-vus-${TARGET_VUS}.json"

  if [[ "${LAB6_AUTO_PLOT:-0}" == "1" ]]; then
    plot_py="${ROOT}/k6/plot_lab6_from_results.py"
    if [[ ! -f "$plot_py" ]]; then
      echo "LAB6_AUTO_PLOT=1: нет $plot_py" >&2
      exit 1
    fi
    mkdir -p "${ROOT}/png_k6"
    echo "LAB6_AUTO_PLOT: python3 $plot_py ${ROOT}/results -o ${ROOT}/png_k6 --vus ${TARGET_VUS}"
    python3 "$plot_py" "${ROOT}/results" -o "${ROOT}/png_k6" --vus "${TARGET_VUS}"
  fi
else
  echo "Подсказка: задайте RESULT_CPU=0.5|1.0|1.5|2 — имена zil-стиля в ${LAB6_REPORTS_DIR}/ и копия в results/cpu-*."
  if [[ "${LAB6_AUTO_PLOT:-0}" == "1" ]]; then
    echo "Подсказка: LAB6_AUTO_PLOT имеет смысл вместе с RESULT_CPU." >&2
  fi
fi

if [[ "${LAB6_AUTO_PLOT_PANELS:-0}" == "1" ]]; then
  plot_panels="${ROOT}/k6/plot_k6_reports.py"
  if [[ ! -f "$plot_panels" ]]; then
    echo "LAB6_AUTO_PLOT_PANELS: нет $plot_panels" >&2
    exit 1
  fi
  echo "LAB6_AUTO_PLOT_PANELS: python3 $plot_panels --lab6 $LAB6_REPORTS_DIR"
  python3 "$plot_panels" --lab6 "$LAB6_REPORTS_DIR" || {
    echo "Панельный график: нужны все файлы cpu05,10,15,20 × mix05,50,95 в ${LAB6_REPORTS_DIR}" >&2
  }
fi

echo "Панельный график (как у одногруппниц): python3 k6/plot_k6_reports.py --lab6 k6/${LAB6_REPORT_SUBDIR}"
