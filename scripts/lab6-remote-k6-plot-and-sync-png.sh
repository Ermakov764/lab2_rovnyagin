#!/usr/bin/env bash
# С ПК: по SSH на k6-ВМ запускает sweep (без plot на ВМ), затем подтягивает results/
# и строит PNG локально — тот же pipeline, что ./scripts/lab6-sync-png-from-k6-vm.sh.
#
# Обязательно задайте BASE_URL — URL приложения с точки зрения k6-ВМ.
#
# Пример:
#   export BASE_URL=http://10.60.3.33:8080
#   export RESULT_CPU=2
#   ./scripts/lab6-remote-k6-plot-and-sync-png.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

K6_SSH_HOST="${K6_SSH_HOST:-hlssh.zil.digital}"
K6_SSH_USER="${K6_SSH_USER:-hl}"
K6_SSH_PORT="${K6_SSH_PORT:-2311}"
K6_REMOTE_SUBDIR="${K6_REMOTE_SUBDIR:-ermakov_k6}"

BASE_URL="${BASE_URL:?Задайте BASE_URL (доступен с k6-ВМ)}"
RESULT_CPU="${RESULT_CPU:?Задайте RESULT_CPU: 0.5 | 1.0 | 1.5 | 2}"
TARGET_VUS="${TARGET_VUS:-30}"
DURATION="${DURATION:-90s}"
FILM_ID="${FILM_ID:-1}"
K6_ROUTE="${K6_ROUTE:-server-to-server}"

echo "SSH ${K6_SSH_USER}@${K6_SSH_HOST}:${K6_SSH_PORT} → ~/${K6_REMOTE_SUBDIR} (только k6 sweep, без plot на ВМ) ..."
ssh -p "${K6_SSH_PORT}" "${K6_SSH_USER}@${K6_SSH_HOST}" bash -s <<EOF
set -euo pipefail
cd "\${HOME}/${K6_REMOTE_SUBDIR}"
export BASE_URL="${BASE_URL}"
export TARGET_VUS="${TARGET_VUS}"
export DURATION="${DURATION}"
export FILM_ID="${FILM_ID}"
export K6_ROUTE="${K6_ROUTE}"
export RESULT_CPU="${RESULT_CPU}"
./k6/run-lab6-ratio-sweep.sh
EOF

echo ""
export BASE_URL DURATION K6_ROUTE FILM_ID
export LAB6_META_BASE_URL="${BASE_URL}"
export LAB6_META_DURATION="${DURATION}"
export LAB6_META_K6_ROUTE="${K6_ROUTE}"
export LAB6_META_FILM_ID="${FILM_ID}"
export LAB6_PLOT_VUS="${TARGET_VUS}"
export K6_REMOTE_DIR="${K6_REMOTE_SUBDIR}"
"${ROOT}/scripts/lab6-sync-png-from-k6-vm.sh"
