#!/usr/bin/env bash
# Перенесён в k6/lab9-hl11-run-all.sh — обёртка для старых путей.
set -euo pipefail
exec "$(cd "$(dirname "$0")" && pwd)/../k6/lab9-hl11-run-all.sh" "$@"
