#!/usr/bin/env bash
set -euo pipefail

# Lab9 full runner for hl11 (k6 VM).
# Runs all 6 cases:
#   CPU 0.5/1.0 x ratios 5/95, 50/50, 95/5
# and collects:
#   - k6 summaries (via run-ratio-sweep)
#   - observability JSON (main/additional)
#   - optional docker logs from hl03 over ssh
#
# Required:
#   - run-ratio-sweep script available
#   - collect-observability-after-k6.sh available
#   - MAIN_BASE_URL / ADDITIONAL_BASE_URL reachable from hl11
#
# Optional env:
#   K6_ROOT=$HOME/ermakov_k6
#   RUN_SWEEP_SCRIPT=$K6_ROOT/k6/run-ratio-sweep.sh
#   COLLECT_SCRIPT=$K6_ROOT/collect-observability-after-k6.sh
#   MAIN_BASE_URL=http://10.60.3.33:8080
#   ADDITIONAL_BASE_URL=http://10.60.3.33:8081
#   TARGET_VUS=30
#   DURATION=90s
#   K6_ROUTE=server-to-server
#   ENABLE_REMOTE_LOGS=1
#   HL03_SSH_HOST=hl@10.60.3.33
#   HL03_REPO_DIR=~/lab2_rovnyagin
#   REMOTE_CPU_SWITCH_SCRIPT=~/lab2_rovnyagin/scripts/lab9-hl3.sh

K6_ROOT="${K6_ROOT:-$HOME/ermakov_k6}"
RUN_SWEEP_SCRIPT="${RUN_SWEEP_SCRIPT:-$K6_ROOT/k6/run-ratio-sweep.sh}"
COLLECT_SCRIPT="${COLLECT_SCRIPT:-$K6_ROOT/collect-observability-after-k6.sh}"

if [[ ! -f "$COLLECT_SCRIPT" && -f "$K6_ROOT/k6/collect-observability-after-k6.sh" ]]; then
  COLLECT_SCRIPT="$K6_ROOT/k6/collect-observability-after-k6.sh"
fi

MAIN_BASE_URL="${MAIN_BASE_URL:-http://10.60.3.33:8080}"
ADDITIONAL_BASE_URL="${ADDITIONAL_BASE_URL:-http://10.60.3.33:8081}"
TARGET_VUS="${TARGET_VUS:-30}"
DURATION="${DURATION:-90s}"
K6_ROUTE="${K6_ROUTE:-server-to-server}"

ENABLE_REMOTE_LOGS="${ENABLE_REMOTE_LOGS:-1}"
HL03_SSH_HOST="${HL03_SSH_HOST:-hl@10.60.3.33}"
HL03_REPO_DIR="${HL03_REPO_DIR:-~/lab2_rovnyagin}"
REMOTE_CPU_SWITCH_SCRIPT="${REMOTE_CPU_SWITCH_SCRIPT:-$HL03_REPO_DIR/scripts/lab9-hl3.sh}"

mkdir -p "$K6_ROOT/results" "$K6_ROOT/logs"
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

collect_remote_logs() {
  local stamp="$1"
  ssh "$HL03_SSH_HOST" "bash $REMOTE_CPU_SWITCH_SCRIPT logs $stamp"
  scp "$HL03_SSH_HOST:$HL03_REPO_DIR/logs/main-$stamp.log" "$K6_ROOT/logs/"
  scp "$HL03_SSH_HOST:$HL03_REPO_DIR/logs/additional-$stamp.log" "$K6_ROOT/logs/"
}

run_case() {
  local cpu="$1" post="$2" get="$3" post_name="$4" get_name="$5"
  local stamp="cpu-${cpu}-ratio-${post_name}-${get_name}-$(date -u +%Y%m%dT%H%M%SZ)"

  echo
  echo "== run case: cpu=${cpu}, ratio ${post_name}/${get_name} =="

  export RESULT_CPU="$cpu"
  export POST_POOL_RATIO="$post"
  export GET_POOL_RATIO="$get"
  export TARGET_VUS DURATION K6_ROUTE
  export BASE_URL_MAIN="$MAIN_BASE_URL"
  export BASE_URL_ADDITIONAL="$ADDITIONAL_BASE_URL"

  bash "$RUN_SWEEP_SCRIPT"

  MAIN_BASE_URL="$MAIN_BASE_URL" \
  ADDITIONAL_BASE_URL="$ADDITIONAL_BASE_URL" \
  OUT_DIR="$K6_ROOT/results/cpu-$cpu" \
  STAMP="$stamp" \
  bash "$COLLECT_SCRIPT"

  if [[ "$ENABLE_REMOTE_LOGS" == "1" ]]; then
    collect_remote_logs "$stamp"
  fi
}

run_cpu_block() {
  local cpu="$1"
  set_remote_cpu "$cpu"
  run_case "$cpu" "0.05" "0.95" "5" "95"
  run_case "$cpu" "0.50" "0.50" "50" "50"
  run_case "$cpu" "0.95" "0.05" "95" "5"
}

preflight
run_cpu_block "0.5"
run_cpu_block "1.0"

echo
echo "All 6 runs completed."
echo "Artifacts:"
echo "  $K6_ROOT/results/cpu-0.5"
echo "  $K6_ROOT/results/cpu-1.0"
echo "  $K6_ROOT/logs"
