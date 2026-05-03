#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Зачем: если PNG уже построены на k6-ВМ — забрать только картинки, без results/ и без локального plot.
# Когда: на сервере после k6 лежат готовые png_k6/lab6-vs-cpu-*.png, нужны те же файлы на ноутбуке.
# Где: ПК; очищает локальный png_k6/lab6-vs-cpu-*.png и копирует с удалённого png_k6 по scp.
# Не подходит для lab8 — для lab8 используйте sync-results-from-k6-vm.sh + lab8-plot-png.sh.
# -----------------------------------------------------------------------------
# Удаляет локальные графики лаб.6 и копирует lab6-vs-cpu-*.png с k6-ВМ.
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
echo "Очистка ${PNG_DIR}/lab6-vs-cpu-*.png ..."
rm -f "${PNG_DIR}"/lab6-vs-cpu-*.png

REMOTE="${K6_SSH_USER}@${K6_SSH_HOST}:~/${K6_REMOTE_SUBDIR}/png_k6"
echo "Копирование с ${REMOTE}/lab6-vs-cpu-*.png (порт ${K6_SSH_PORT}) ..."
set +e
scp -P "${K6_SSH_PORT}" "${REMOTE}/lab6-vs-cpu-*.png" "${PNG_DIR}/"
set -e
echo "Готово:"
if ! ls -la "${PNG_DIR}"/lab6-vs-cpu-*.png 2>/dev/null; then
  echo "Нет файлов lab6-vs-cpu-*.png — на ВМ сначала plot или проверьте путь." >&2
  exit 1
fi
