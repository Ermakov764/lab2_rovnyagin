#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Зачем: не вводить длинный rsync вручную — подтянуть k6/results с k6-ВМ на этот ПК.
# Когда: после прогонов на ВМ k6 нужно обновить локальные JSON перед plot или отчётом.
# Где: обычно с ноутбука (не с hl03), где настроен SSH Host hl-k6 в ~/.ssh/config.
# Типовой сценарий: ./k6/sync-results-from-k6-vm.sh && ./k6/plot-png.sh <TARGET_VUS>
# -----------------------------------------------------------------------------
# Зеркально подтянуть каталог k6/results/ с k6-ВМ в локальный ./k6/results
# (те же JSON, что после прогонов k6; затем ./k6/plot-from-results.sh или ./k6/plot-png.sh <vus>).
#
# Нужен SSH-алиас hl-k6 в ~/.ssh/config (HostName hlssh.zil.digital, Port 2311, User hl).
# Пароль вместо ключа: RSH_CMD='ssh -o BatchMode=no' ./k6/sync-results-from-k6-vm.sh
#
# Переменные:
#   K6_RESULTS_REMOTE  — источник rsync (по умолчанию hl-k6:~/ermakov_k6/k6/results)
#   RSH_CMD            — команда для rsync -e (по умолчанию ssh)
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

REMOTE_BASE="${K6_RESULTS_REMOTE:-hl-k6:~/ermakov_k6/k6/results}"
# Нормализуем один завершающий слэш у базы remote для копирования содержимого в ./k6/results/
REMOTE="${REMOTE_BASE%/}/"
RSH_CMD="${RSH_CMD:-ssh}"

mkdir -p "${ROOT}/k6/results"

echo "==> rsync -avz --delete -e ${RSH_CMD} ${REMOTE} -> ${ROOT}/k6/results/"
rsync -avz --delete -e "${RSH_CMD}" "${REMOTE}" "${ROOT}/k6/results/"
echo "Готово: ${ROOT}/k6/results/ (зеркало как на k6). Дальше: ./k6/plot-from-results.sh или ./k6/plot-png.sh <vus>"
