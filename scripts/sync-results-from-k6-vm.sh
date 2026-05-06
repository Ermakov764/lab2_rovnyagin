#!/usr/bin/env bash
# Перенесён в k6/sync-results-from-k6-vm.sh — обёртка для старых путей.
set -euo pipefail
exec "$(cd "$(dirname "$0")" && pwd)/../k6/sync-results-from-k6-vm.sh" "$@"
