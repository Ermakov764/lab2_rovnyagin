#!/usr/bin/env bash
# После прогона k6 на k6-ВМ: построить PNG там и скопировать в ./png_k6/ на этом ПК.
# Запускать ТОЛЬКО с вашего компьютера, из корня репозитория:
#   ./scripts/lab6-sync-png-from-k6-vm.sh
#
# Переменные при необходимости:
#   K6_SSH_HOST   (по умолчанию hlssh.zil.digital)
#   K6_SSH_PORT   (по умолчанию 2311)
#   K6_SSH_USER   (по умолчанию hl)
#   K6_REMOTE_DIR (по умолчанию ermakov_k6)
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PNG_DIR="${ROOT}/png_k6"
mkdir -p "${PNG_DIR}"

K6_SSH_HOST="${K6_SSH_HOST:-hlssh.zil.digital}"
K6_SSH_PORT="${K6_SSH_PORT:-2311}"
K6_SSH_USER="${K6_SSH_USER:-hl}"
K6_REMOTE_DIR="${K6_REMOTE_DIR:-ermakov_k6}"
REMOTE="${K6_SSH_USER}@${K6_SSH_HOST}"

ssh -p "${K6_SSH_PORT}" "${REMOTE}" env RDIR="${K6_REMOTE_DIR}" bash -c '
set -euo pipefail
R="${HOME}/${RDIR}"
P="${R}/k6/plot_lab6_from_results.py"
if [[ ! -f "${P}" ]]; then
  echo "Нет ${P}. Положите plot_lab6_from_results.py на k6-ВМ." >&2
  exit 1
fi
python3 "${P}" "${R}/results" -o "${R}/png_k6"
'

rm -f "${PNG_DIR}"/lab6-vs-cpu-*.png "${PNG_DIR}"/lab6-cpu-*.png
scp -P "${K6_SSH_PORT}" "${REMOTE}:${K6_REMOTE_DIR}/png_k6/lab6-vs-cpu-*.png" "${PNG_DIR}/"
echo "Готово: ${PNG_DIR}/lab6-vs-cpu-*.png"
