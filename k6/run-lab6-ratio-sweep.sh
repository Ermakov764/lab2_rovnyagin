#!/usr/bin/env bash
# LAB6: три прогона с постоянными VU и разными POST_SHARE (5/95, 50/50, 95/5).
# Результаты: k6/reports/lab6-summary-*.json
#
# Опционально RESULT_CPU — после прогона JSON копируются в results/cpu-<метка>/
# (старые файлы в этой папке удаляются). Метки каталогов: 0.5, 1.0, 1.5, 2
# Примеры: RESULT_CPU=1.5  или  RESULT_CPU=2
#
# Опционально LAB6_AUTO_PLOT=1 — после прогона вызвать k6/plot_lab6_from_results.py
# (нужен matplotlib; на ВМ должен лежать plot_lab6_from_results.py в k6/).
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p k6/reports

# Всегда готовим дерево под отчёты лаб.6 (общая k6-ВМ: ~/ermakov_k6)
mkdir -p \
  "${ROOT}/results/cpu-0.5" \
  "${ROOT}/results/cpu-1.0" \
  "${ROOT}/results/cpu-1.5" \
  "${ROOT}/results/cpu-2"

BASE_URL="${BASE_URL:-http://localhost:8080}"
TARGET_VUS="${TARGET_VUS:-30}"
DURATION="${DURATION:-90s}"
FILM_ID="${FILM_ID:-1}"

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

run_one() {
  local share="$1"
  local tag="$2"
  echo "=== POST_SHARE=$share ($tag) TARGET_VUS=$TARGET_VUS DURATION=$DURATION ==="
  k6 run --summary-export "k6/reports/lab6-summary-${tag}-vus-${TARGET_VUS}.json" \
    -e "BASE_URL=$BASE_URL" \
    -e "TARGET_VUS=$TARGET_VUS" \
    -e "POST_SHARE=$share" \
    -e "DURATION=$DURATION" \
    -e "FILM_ID=$FILM_ID" \
    k6/cinema-lab6-constant.js
}

run_one 0.05 "post05-get95"
run_one 0.50 "post50-get50"
run_one 0.95 "post95-get05"

echo "Готово. JSON в k6/reports/lab6-summary-*.json"

if [[ -n "${RESULT_CPU:-}" ]]; then
  label="$(normalize_result_cpu "${RESULT_CPU}")"
  if [[ -z "$label" ]]; then
    echo "Ошибка: RESULT_CPU='${RESULT_CPU}' — ожидается одно из: 0.5, 1.0, 1.5, 2 (или 2.0)" >&2
    exit 1
  fi
  dest="${ROOT}/results/cpu-${label}"
  mkdir -p "$dest"
  shopt -s nullglob
  rm -f "${dest}"/lab6-summary-*.json
  copies=(k6/reports/lab6-summary-*.json)
  if [[ ${#copies[@]} -eq 0 ]]; then
    echo "Ошибка: в k6/reports нет lab6-summary-*.json для копирования." >&2
    exit 1
  fi
  cp -v "${copies[@]}" "$dest/"
  echo "Скопировано в ${dest}/ (папка перед копированием очищена от старых lab6-summary-*.json)."

  if [[ "${LAB6_AUTO_PLOT:-0}" == "1" ]]; then
    plot_py="${ROOT}/k6/plot_lab6_from_results.py"
    if [[ ! -f "$plot_py" ]]; then
      echo "LAB6_AUTO_PLOT=1: нет $plot_py — положите скрипт или отключите LAB6_AUTO_PLOT." >&2
      exit 1
    fi
    mkdir -p "${ROOT}/png_k6"
    echo "LAB6_AUTO_PLOT: python3 $plot_py ${ROOT}/results -o ${ROOT}/png_k6"
    python3 "$plot_py" "${ROOT}/results" -o "${ROOT}/png_k6"
  fi
else
  echo "Подсказка: задайте RESULT_CPU=0.5|1.0|1.5|2 — тогда JSON продублируются в results/cpu-<метка>/."
  if [[ "${LAB6_AUTO_PLOT:-0}" == "1" ]]; then
    echo "Подсказка: LAB6_AUTO_PLOT имеет смысл вместе с RESULT_CPU (иначе свежие JSON только в k6/reports)." >&2
  fi
fi
