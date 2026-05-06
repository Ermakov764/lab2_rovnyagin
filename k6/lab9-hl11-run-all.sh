#!/usr/bin/env bash
set -euo pipefail

# Lab9 / Lab13: один запуск — CPU на hl03 + полный sweep k6 + observability + логи.
# Запускать на ВМ с k6 (рядом с этим каталогом), из корня репозитория: ./k6/lab9-hl11-run-all.sh
#
# Матрица для графиков (k6/plot_k6_cpu_results.py):
#   • На каждом CPU (0.5 и 1.0) один прогон run-ratio-sweep.sh — внутри три смеси POST/GET:
#       5/95, 50/50, 95/5  →  summary-post05-get95-…, post50-get50, post95-get05
#   • Итого 2×(3 k6) = 6 сценариев k6; summaries в <results-parent>/cpu-0.5 и cpu-1.0 по три JSON.
#
# LAB13 (два прогона concurrency=1 и 2): задай разные каталоги результатов, например:
#   K6_RESULTS_SUFFIX=conc-1 AUTO_PLOT=1 ./k6/lab9-hl11-run-all.sh
#   # на hl03: KAFKA_LISTENER_CONCURRENCY=2 + compose recreate
#   K6_RESULTS_SUFFIX=conc-2 AUTO_PLOT=1 ./k6/lab9-hl11-run-all.sh
# Родитель каталогов: k6/results-<suffix>/ (по умолчанию без суффикса — k6/results/).
# PNG: префикс vs-cpu-<suffix> (или K6_PNG_PREFIX); заголовок — K6_PLOT_TITLE_TAG или суффикс в подзаголовке.
#
# Требуется: k6, curl, ssh/scp к hl03; на hl03 — scripts/lab9-hl3.sh и compose.
#
# Переменные окружения (часто достаточно умолчаний):
#   K6_ROOT          — корень репозитория с подкаталогом k6/ (по умолчанию: родитель каталога k6/)
#   K6_RESULTS_BASE  — явный каталог-родитель cpu-0.5 / cpu-1.0 (перекрывает K6_RESULTS_SUFFIX)
#   K6_RESULTS_SUFFIX — суффикс: k6/results-<suffix>/ (не перезаписывает другой прогон)
#   K6_PNG_OUT_DIR   — каталог для PNG (по умолчанию k6/png_k6/)
#   K6_PNG_PREFIX    — префикс имён PNG (если не задан и есть K6_RESULTS_SUFFIX — vs-cpu-<suffix>)
#   K6_PLOT_TITLE_TAG — первая строка заголовка на всех трёх графиках (иначе дефолт + run=<suffix>)
#   MAIN_BASE_URL, ADDITIONAL_BASE_URL
#   TARGET_VUS, DURATION, K6_ROUTE
#   BASE_URL_KAFKA_PROXY, K6_WRITE_MODE  — LAB13 (как в k6/env.sh)
#   HL03_SSH_HOST, HL03_REPO_DIR, REMOTE_CPU_SWITCH_SCRIPT
#   ENABLE_REMOTE_LOGS=0 — без docker logs по ssh
#   AUTO_PLOT=1        — в конце вызвать k6/plot-from-results.sh → k6/png_k6/ (не внутри каждого sweep)
#   WAIT_AFTER_CPU_RECREATE_SEC — макс. секунд ожидания ping/observability после set-cpu (по умолчанию 180)

_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
_REPO_ROOT="$(cd "$_SCRIPT_DIR/.." && pwd)"
K6_ROOT="${K6_ROOT:-$_REPO_ROOT}"

RUN_SWEEP_SCRIPT="${RUN_SWEEP_SCRIPT:-$K6_ROOT/k6/run-ratio-sweep.sh}"
COLLECT_SCRIPT="${COLLECT_SCRIPT:-$K6_ROOT/k6/collect-observability-after-k6.sh}"

if [[ ! -f "$COLLECT_SCRIPT" && -f "$K6_ROOT/k6/collect-observability-after-k6.sh" ]]; then
  COLLECT_SCRIPT="$K6_ROOT/k6/collect-observability-after-k6.sh"
fi

MAIN_BASE_URL="${MAIN_BASE_URL:-http://10.60.3.33:8080}"
ADDITIONAL_BASE_URL="${ADDITIONAL_BASE_URL:-http://10.60.3.33:8081}"
TARGET_VUS="${TARGET_VUS:-30}"
DURATION="${DURATION:-90s}"
K6_ROUTE="${K6_ROUTE:-server-to-server}"

BASE_URL_KAFKA_PROXY="${BASE_URL_KAFKA_PROXY:-http://127.0.0.1:8082}"
K6_WRITE_MODE="${K6_WRITE_MODE:-kafka}"

ENABLE_REMOTE_LOGS="${ENABLE_REMOTE_LOGS:-1}"
HL03_SSH_HOST="${HL03_SSH_HOST:-hl@10.60.3.33}"
HL03_REPO_DIR="${HL03_REPO_DIR:-~/lab2_rovnyagin}"
REMOTE_CPU_SWITCH_SCRIPT="${REMOTE_CPU_SWITCH_SCRIPT:-$HL03_REPO_DIR/scripts/lab9-hl3.sh}"

# Родитель k6/results/.../cpu-0.5|cpu-1.0
resolve_k6_results_base() {
  if [[ -n "${K6_RESULTS_BASE:-}" ]]; then
    printf '%s' "$K6_RESULTS_BASE"
    return
  fi
  if [[ -n "${K6_RESULTS_SUFFIX:-}" ]]; then
    local s="${K6_RESULTS_SUFFIX#/}"
    s="${s%/}"
    if [[ -z "$s" ]]; then
      echo "Ошибка: K6_RESULTS_SUFFIX пустой после нормализации" >&2
      exit 1
    fi
    printf '%s' "$K6_ROOT/k6/results-${s}"
    return
  fi
  printf '%s' "$K6_ROOT/k6/results"
}

export K6_RESULTS_BASE="$(resolve_k6_results_base)"

mkdir -p "$K6_RESULTS_BASE/cpu-0.5" "$K6_RESULTS_BASE/cpu-1.0" "$K6_ROOT/k6/logs"
chmod +x "$RUN_SWEEP_SCRIPT" "$COLLECT_SCRIPT"

preflight() {
  echo "== preflight =="
  command -v k6 >/dev/null 2>&1 || {
    echo "k6 is not installed or not in PATH"
    exit 1
  }
  curl -sf "${MAIN_BASE_URL}/api/cinema/ping" >/dev/null || {
    echo "Main service is not reachable: ${MAIN_BASE_URL}"
    exit 1
  }
  curl -sf "${MAIN_BASE_URL}/api/observability" >/dev/null || {
    echo "Main observability endpoint unavailable: ${MAIN_BASE_URL}/api/observability"
    exit 1
  }
  curl -sf "${ADDITIONAL_BASE_URL}/api/observability" >/dev/null || {
    echo "Additional observability endpoint unavailable: ${ADDITIONAL_BASE_URL}/api/observability"
    exit 1
  }
  if [[ "$ENABLE_REMOTE_LOGS" == "1" ]]; then
    ssh "$HL03_SSH_HOST" "echo remote-ok" >/dev/null || {
      echo "SSH to hl03 failed: $HL03_SSH_HOST"
      exit 1
    }
  fi
}

set_remote_cpu() {
  local cpu="$1"
  ssh "$HL03_SSH_HOST" "bash $REMOTE_CPU_SWITCH_SCRIPT set-cpu $cpu"
}

# После set-cpu на hl03 идёт force-recreate контейнеров — порт 8080 некоторое время закрыт.
wait_ready_after_cpu_change() {
  local max="${WAIT_AFTER_CPU_RECREATE_SEC:-180}"
  local deadline=$((SECONDS + max))
  echo "== ждём main/additional после set-cpu (до ${max}s) =="
  while (( SECONDS < deadline )); do
    if curl -sf "${MAIN_BASE_URL}/api/cinema/ping" >/dev/null \
      && curl -sf "${MAIN_BASE_URL}/api/observability" >/dev/null \
      && curl -sf "${ADDITIONAL_BASE_URL}/api/observability" >/dev/null; then
      echo "== сервисы отвечают, дальше k6 =="
      return 0
    fi
    echo "... compose ещё поднимается, пауза 3s"
    sleep 3
  done
  echo "Таймаут: за ${max}s после set-cpu не удалось достучаться до ${MAIN_BASE_URL} / ${ADDITIONAL_BASE_URL}" >&2
  exit 1
}

collect_remote_logs() {
  local stamp="$1"
  ssh "$HL03_SSH_HOST" "bash $REMOTE_CPU_SWITCH_SCRIPT logs $stamp"
  scp "$HL03_SSH_HOST:$HL03_REPO_DIR/logs/main-$stamp.log" "$K6_ROOT/k6/logs/"
  scp "$HL03_SSH_HOST:$HL03_REPO_DIR/logs/additional-$stamp.log" "$K6_ROOT/k6/logs/"
}

# Один CPU: выставить лимиты на hl03, три смеси внутри run-ratio-sweep, снимок observability, логи.
run_sweep_for_cpu() {
  local cpu="$1"
  local stamp="cpu-${cpu}-sweep-$(date -u +%Y%m%dT%H%M%SZ)"

  echo
  echo "== CPU ${cpu}: remote set-cpu, k6 sweep (5/95, 50/50, 95/5), collect =="

  set_remote_cpu "$cpu"
  wait_ready_after_cpu_change

  export RESULT_CPU="$cpu"
  export TARGET_VUS DURATION K6_ROUTE
  export BASE_URL_MAIN="$MAIN_BASE_URL"
  export BASE_URL_ADDITIONAL="$ADDITIONAL_BASE_URL"
  export BASE_URL_KAFKA_PROXY K6_WRITE_MODE

  # Не прокидывать AUTO_PLOT в run-ratio-sweep: там plot после одного CPU, а нужны cpu-0.5 и cpu-1.0.
  AUTO_PLOT=0 bash "$RUN_SWEEP_SCRIPT"

  MAIN_BASE_URL="$MAIN_BASE_URL" \
  ADDITIONAL_BASE_URL="$ADDITIONAL_BASE_URL" \
  OUT_DIR="$K6_RESULTS_BASE/cpu-$cpu" \
  STAMP="$stamp" \
  bash "$COLLECT_SCRIPT"

  if [[ "$ENABLE_REMOTE_LOGS" == "1" ]]; then
    collect_remote_logs "$stamp"
  fi
}

maybe_plot() {
  if [[ "${AUTO_PLOT:-0}" != "1" ]]; then
    return 0
  fi
  local plot_sh="$K6_ROOT/k6/plot-from-results.sh"
  [[ -f "$plot_sh" ]] || {
    echo "AUTO_PLOT=1 but missing: $plot_sh" >&2
    return 1
  }
  chmod +x "$plot_sh"
  local png_out="${K6_PNG_OUT_DIR:-$K6_ROOT/k6/png_k6}"
  local png_pre
  if [[ -n "${K6_PNG_PREFIX+x}" ]]; then
    png_pre="${K6_PNG_PREFIX:-vs-cpu}"
  elif [[ -n "${K6_RESULTS_SUFFIX:-}" ]]; then
    local _s="${K6_RESULTS_SUFFIX#/}"
    _s="${_s%/}"
    png_pre="vs-cpu-${_s//\//-}"
  else
    png_pre="vs-cpu"
  fi
  local title="${K6_PLOT_TITLE_TAG:-}"
  if [[ -z "$title" ]]; then
    title="POST запись (Kafka) / GET summary"
    if [[ -n "${K6_RESULTS_SUFFIX:-}" ]]; then
      title+="; run=${K6_RESULTS_SUFFIX}"
    fi
  fi
  echo
  echo "== AUTO_PLOT: $plot_sh RESULTS=$K6_RESULTS_BASE OUT=$png_out prefix=$png_pre =="
  TITLE_TAG="$title" K6_PNG_PREFIX="$png_pre" TARGET_VUS="$TARGET_VUS" \
    bash "$plot_sh" "$K6_RESULTS_BASE" "$png_out"
}

preflight
run_sweep_for_cpu "0.5"
run_sweep_for_cpu "1.0"
maybe_plot

echo
echo "Готово: 2 блока CPU × 3 смеси k6; артефакты для трёх графиков (ось X = 0.5 и 1.0 vCPU)."
echo "  $K6_RESULTS_BASE/cpu-0.5"
echo "  $K6_RESULTS_BASE/cpu-1.0"
echo "  $K6_ROOT/k6/logs"
echo "Графики (LAB13 по умолчанию — легенды kafka-proxy в k6/plot-from-results.sh):"
echo "  TARGET_VUS=$TARGET_VUS K6_PNG_PREFIX=${K6_PNG_PREFIX:-vs-cpu} bash $K6_ROOT/k6/plot-from-results.sh $K6_RESULTS_BASE ${K6_PNG_OUT_DIR:-$K6_ROOT/k6/png_k6}"
echo "Только если прогон был K6_WRITE_MODE=rest (POST в CRUD), подписи вручную:"
echo "  POST_LEGEND='POST /api/viewers (среднее, мс)' TITLE_TAG='POST viewers / GET summary' TARGET_VUS=$TARGET_VUS bash $K6_ROOT/k6/plot-from-results.sh $K6_RESULTS_BASE ${K6_PNG_OUT_DIR:-$K6_ROOT/k6/png_k6}"
