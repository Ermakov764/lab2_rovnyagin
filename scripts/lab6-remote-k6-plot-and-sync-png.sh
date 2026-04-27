#!/usr/bin/env bash
# С ПК: по SSH на k6-ВМ запускает sweep + LAB6_AUTO_PLOT, затем подтягивает PNG в ./png_k6
# (старые lab6-vs-cpu-*.png локально удаляются в sync-скрипте).
#
# Обязательно задайте BASE_URL — URL приложения с точки зрения k6-ВМ (IP hl03 и т. п.).
#
# Пример:
#   export BASE_URL=http://192.168.1.242:8080
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

echo "SSH ${K6_SSH_USER}@${K6_SSH_HOST}:${K6_SSH_PORT} → ~/${K6_REMOTE_SUBDIR} (k6 + plot) ..."
ssh -p "${K6_SSH_PORT}" "${K6_SSH_USER}@${K6_SSH_HOST}" bash -s <<EOF
set -euo pipefail
cd "\${HOME}/${K6_REMOTE_SUBDIR}"
export BASE_URL="${BASE_URL}"
export TARGET_VUS="${TARGET_VUS}"
export DURATION="${DURATION}"
export FILM_ID="${FILM_ID}"
export RESULT_CPU="${RESULT_CPU}"
export LAB6_AUTO_PLOT=1
./k6/run-lab6-ratio-sweep.sh
EOF

echo ""
"${ROOT}/scripts/sync-lab6-png-from-k6-vm.sh"
