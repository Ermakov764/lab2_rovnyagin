#!/usr/bin/env bash
# С ПК: удаляет локальные графики лаб.6 в png_k6 и копирует свежие lab6-vs-cpu-*.png с k6-ВМ.
#
# Переменные окружения (опционально):
#   K6_SSH_HOST       — по умолчанию hlssh.zil.digital
#   K6_SSH_USER       — по умолчанию hl
#   K6_SSH_PORT       — SSH k6-ВМ из таблицы (по умолчанию 2311)
#   K6_REMOTE_SUBDIR  — каталог от $HOME на ВМ (по умолчанию ermakov_k6)
#
# Пример:
#   ./scripts/sync-lab6-png-from-k6-vm.sh
#   K6_SSH_PORT=2311 ./scripts/sync-lab6-png-from-k6-vm.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PNG_DIR="${LAB6_PNG_DIR:-${ROOT}/png_k6}"

K6_SSH_HOST="${K6_SSH_HOST:-hlssh.zil.digital}"
K6_SSH_USER="${K6_SSH_USER:-hl}"
K6_SSH_PORT="${K6_SSH_PORT:-2311}"
K6_REMOTE_SUBDIR="${K6_REMOTE_SUBDIR:-ermakov_k6}"

mkdir -p "$PNG_DIR"
echo "Очистка ${PNG_DIR}/lab6-vs-cpu-*.png (и устаревших lab6-cpu-*.png) ..."
rm -f "${PNG_DIR}"/lab6-vs-cpu-*.png "${PNG_DIR}"/lab6-cpu-*.png

REMOTE_SPEC="${K6_SSH_USER}@${K6_SSH_HOST}:~/${K6_REMOTE_SUBDIR}/png_k6/lab6-vs-cpu-*.png"
echo "Копирование с ${REMOTE_SPEC} (порт ${K6_SSH_PORT}) ..."
scp -P "${K6_SSH_PORT}" "${REMOTE_SPEC}" "${PNG_DIR}/"

echo "Готово:"
ls -la "${PNG_DIR}"/lab6-vs-cpu-*.png 2>/dev/null || {
  echo "Нет файлов lab6-vs-cpu-*.png — на ВМ сначала plot_lab6_from_results.py или проверьте путь." >&2
  exit 1
}
