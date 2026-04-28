#!/usr/bin/env bash
# Один сценарий: с k6-ВМ → ./results → при необходимости lab6_meta → ./png_k6/*.png
# Реализация: обёртка над scripts/lab6-sync-png-from-k6-vm.sh
#
# Минимум (тот же BASE_URL, что у k6 — для подписей и inject старых JSON):
#   export BASE_URL=http://192.168.1.242:8080
#   export LAB6_PLOT_VUS=400   # если в results/cpu-* есть и *-vus-30.json и *-vus-400.json
#   ./scripts/lab6-get-png-from-k6-vm.sh
#
# См. также scripts/lab6-sync-png-from-k6-vm.sh (переменные K6_SSH_*, K6_SSH_BATCH_MODE=0 и т.д.)
#
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
exec "${ROOT}/scripts/lab6-sync-png-from-k6-vm.sh"
