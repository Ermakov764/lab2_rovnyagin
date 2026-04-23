#!/usr/bin/env bash
# LAB5: запуск seed_rest_data.py (как обёртка над k6/run-sweep.sh для единообразия).
#
# Без аргументов — подставляет значения из переменных окружения (или дефолты):
#   BASE_URL   по умолчанию http://localhost:8080
#   ENDPOINT   по умолчанию all  (films | viewers | tickets | all)
#   COUNT      по умолчанию 500
#
# С аргументами — всё передаётся в Python как есть (нужен --endpoint):
#   ./tools/run-seed.sh --endpoint films --count 100
#   ./tools/run-seed.sh --endpoint tickets --count 100 --divisor 10
#   ./tools/run-seed.sh --endpoint viewers --clear
#
#   CLEAR=1 ENDPOINT=viewers ./tools/run-seed.sh — только очистка (без count)
#
#   NO_PIP_INSTALL=1 — не создавать venv и не ставить пакеты (нужен уже настроенный Python с requests/faker)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT}"

export BASE_URL="${BASE_URL:-http://localhost:8080}"

VENV_PY="${SCRIPT_DIR}/.venv/bin/python3"

pick_python() {
  if [[ -x "${VENV_PY}" ]] && "${VENV_PY}" -c "import requests, faker" 2>/dev/null; then
    echo "${VENV_PY}"
    return 0
  fi
  if python3 -c "import requests, faker" 2>/dev/null; then
    echo python3
    return 0
  fi
  return 1
}

ensure_deps() {
  if [[ "${NO_PIP_INSTALL:-0}" == "1" ]]; then
    return 0
  fi
  if pick_python &>/dev/null; then
    return 0
  fi
  echo "Зависимости requests/faker не найдены — создаю venv: ${SCRIPT_DIR}/.venv"
  python3 -m venv "${SCRIPT_DIR}/.venv"
  "${SCRIPT_DIR}/.venv/bin/pip" install -r "${SCRIPT_DIR}/requirements-seed.txt"
}

ensure_deps

PYBIN="$(pick_python)" || {
  echo "Не удалось найти Python с requests и faker. Установите вручную или уберите NO_PIP_INSTALL=1."
  exit 1
}

if [[ "$#" -eq 0 ]]; then
  ENDPOINT="${ENDPOINT:-all}"
  if [[ "${CLEAR:-0}" == "1" ]]; then
    exec "${PYBIN}" "${SCRIPT_DIR}/seed_rest_data.py" \
      --base-url "${BASE_URL}" \
      --endpoint "${ENDPOINT}" \
      --clear
  fi
  COUNT="${COUNT:-500}"
  exec "${PYBIN}" "${SCRIPT_DIR}/seed_rest_data.py" \
    --base-url "${BASE_URL}" \
    --endpoint "${ENDPOINT}" \
    --count "${COUNT}"
fi

exec "${PYBIN}" "${SCRIPT_DIR}/seed_rest_data.py" "$@"
