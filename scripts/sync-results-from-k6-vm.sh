#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Зачем: не вводить длинный rsync вручную — синхронизировать отчёты k6 с ВМ на ноутбук.
# Когда: после серии прогонов на k6-ВМ нужно обновить локальные JSON перед plot или отчётом.
# Где: с ПК (не с hl03), где настроен SSH Host hl-k6 в ~/.ssh/config.
# Типовой сценарий: ./scripts/sync-results-from-k6-vm.sh && ./scripts/lab8-plot-png.sh <TARGET_VUS>
# -----------------------------------------------------------------------------
# Зеркально подтянуть каталог k6/cpu-runs/ с k6-ВМ в локальный ./k6/cpu-runs
# (те же JSON, что после прогонов k6; затем ./scripts/lab8-plot-png.sh <vus>).
#
# Нужен SSH-алиас hl-k6 в ~/.ssh/config (HostName hlssh.zil.digital, Port 2311, User hl).
# Пароль вместо ключа: RSH_CMD='ssh -o BatchMode=no' ./scripts/sync-results-from-k6-vm.sh
#
# Переменные:
#   K6_RESULTS_REMOTE  — источник rsync (по умолчанию hl-k6:~/ermakov_k6/k6/cpu-runs)
#   RSH_CMD            — команда для rsync -e (по умолчанию ssh)
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Нормализуем один завершающий слэш у базы remote для копирования содержимого в ./k6/cpu-runs/
REMOTE_BASE="${K6_RESULTS_REMOTE:-hl-k6:~/ermakov_k6/k6/cpu-runs}"
REMOTE="${REMOTE_BASE%/}/"
RSH_CMD="${RSH_CMD:-ssh}"

LOCAL_CPU_RUNS="${K6_CPU_RUNS_ROOT:-${ROOT}/k6/cpu-runs}"
mkdir -p "${LOCAL_CPU_RUNS}"

echo "==> rsync -avz --delete -e ${RSH_CMD} ${REMOTE} -> ${LOCAL_CPU_RUNS}/"
rsync -avz --delete -e "${RSH_CMD}" "${REMOTE}" "${LOCAL_CPU_RUNS}/"
echo "Готово: ${LOCAL_CPU_RUNS}/ (зеркало как на k6). Дальше: ./scripts/lab8-plot-png.sh <vus>"
