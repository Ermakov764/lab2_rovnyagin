#!/usr/bin/env bash
# Лаб. 6 (ТЗ): проброс HTTP с персональной ВМ на localhost ПК — как ssh -L 8080:localhost:8080.
# Запускать на своём компьютере. Пока сессия открыта: http://localhost:8080/ и Swagger.
#
# Переменные окружения (опционально):
#   SSH_HOST     — по умолчанию hlssh.zil.digital
#   SSH_PORT     — порт SSH из таблицы курса (по умолчанию 2303)
#   SSH_USER     — по умолчанию hl
#   LOCAL_PORT   — порт на ПК (по умолчанию 8080)
#   REMOTE_HOST  — куда на ВМ стучится туннель (по умолчанию localhost)
#   REMOTE_PORT  — порт приложения на ВМ (по умолчанию 8080)
#
# Доп. аргументы передаются в ssh, например только туннель в фоне:
#   ./scripts/ssh-tunnel-personal-vm.sh -N -f
#
set -euo pipefail

SSH_HOST="${SSH_HOST:-hlssh.zil.digital}"
SSH_PORT="${SSH_PORT:-2303}"
SSH_USER="${SSH_USER:-hl}"
LOCAL_PORT="${LOCAL_PORT:-8080}"
REMOTE_HOST="${REMOTE_HOST:-localhost}"
REMOTE_PORT="${REMOTE_PORT:-8080}"

REMOTE="${SSH_USER}@${SSH_HOST}"

echo "Туннель: http://127.0.0.1:${LOCAL_PORT}/ → на ВМ ${REMOTE_HOST}:${REMOTE_PORT}"
echo "SSH: ${REMOTE} (порт ${SSH_PORT}). Ctrl+C — выход и остановка проброса."
echo ""

exec ssh -p "${SSH_PORT}" -L "${LOCAL_PORT}:${REMOTE_HOST}:${REMOTE_PORT}" "${REMOTE}" "$@"
