#!/usr/bin/env bash
# Лаб. 6–7: с ПК — приложение на персональной ВМ и (опционально) pgAdmin на сервере БД.
#
# По умолчанию поднимаются два туннеля:
#   • http://127.0.0.1:8080/  → приложение на персональной ВМ (лаб. 6)
#   • http://127.0.0.1:5051/  → pgAdmin на hl12 (лаб. 7; SSH порт 2312 из таблицы курса)
#
# Запускать на своём компьютере. Ctrl+C в основном SSH — закрывает оба проброса.
#
# Переменные окружения (опционально):
#   SSH_HOST           — по умолчанию hlssh.zil.digital
#   SSH_PORT           — SSH персональной ВМ (по умолчанию 2303)
#   SSH_USER           — по умолчанию hl
#   LOCAL_PORT         — порт приложения на ПК (по умолчанию 8080)
#   REMOTE_HOST        — на ВМ (по умолчанию localhost)
#   REMOTE_PORT        — порт приложения на ВМ (по умолчанию 8080)
#
#   PGADMIN_TUNNEL     — 0 чтобы не поднимать туннель pgAdmin (только 8080)
#   PGADMIN_SSH_PORT   — SSH сервера БД (по умолчанию 2312)
#   PGADMIN_LOCAL_PORT — порт pgAdmin на ПК (по умолчанию 5051)
#   PGADMIN_SSH_HOST   — по умолчанию как SSH_HOST
#   PGADMIN_SSH_USER   — по умолчанию как SSH_USER
#
#   AUTO_START_REMOTE_APP — 0 чтобы не трогать Docker на ВМ (по умолчанию 1: лаб. 7 app)
#   REMOTE_REPO_SUBDIR    — каталог репозитория относительно $HOME на ВМ (по умолчанию lab2_rovnyagin)
#   LAB7_COMPOSE_FILE     — compose только приложения (по умолчанию docker-compose.lab7-app.yml)
#
# Доп. аргументы передаются в ssh персональной ВМ. При -f туннель pgAdmin не стартует
# (нужен второй терминал или запуск без -f); см. сообщение скрипта.
#
set -euo pipefail

SSH_HOST="${SSH_HOST:-hlssh.zil.digital}"
SSH_PORT="${SSH_PORT:-2303}"
SSH_USER="${SSH_USER:-hl}"
LOCAL_PORT="${LOCAL_PORT:-8080}"
REMOTE_HOST="${REMOTE_HOST:-localhost}"
REMOTE_PORT="${REMOTE_PORT:-8080}"

# С ключом -f основной ssh сразу отпускает терминал — авто-очистка pgAdmin сломает второй туннель.
SSH_EXTRA_HAS_F=0
for a in "$@"; do
  if [[ "$a" == "-f" ]]; then
    SSH_EXTRA_HAS_F=1
    break
  fi
done

PGADMIN_TUNNEL="${PGADMIN_TUNNEL:-1}"
if [[ "${PGADMIN_TUNNEL}" != "0" && "${SSH_EXTRA_HAS_F}" -eq 1 ]]; then
  echo "Замечание: при -f туннель pgAdmin не поднимается (иначе он сразу закроется)." >&2
  echo "  Отдельно: ssh -p ${PGADMIN_SSH_PORT:-2312} -N -L ${PGADMIN_LOCAL_PORT:-5051}:127.0.0.1:5051 ${PGADMIN_SSH_USER:-hl}@${PGADMIN_SSH_HOST:-hlssh.zil.digital}" >&2
  echo "  Или без -f запустите этот скрипт — поднимутся 8080 и 5051." >&2
  PGADMIN_TUNNEL=0
fi

PGADMIN_SSH_PORT="${PGADMIN_SSH_PORT:-2312}"
PGADMIN_LOCAL_PORT="${PGADMIN_LOCAL_PORT:-5051}"
PGADMIN_SSH_HOST="${PGADMIN_SSH_HOST:-$SSH_HOST}"
PGADMIN_SSH_USER="${PGADMIN_SSH_USER:-$SSH_USER}"

REMOTE="${SSH_USER}@${SSH_HOST}"
PGADMIN_REMOTE="${PGADMIN_SSH_USER}@${PGADMIN_SSH_HOST}"

AUTO_START_REMOTE_APP="${AUTO_START_REMOTE_APP:-1}"
REMOTE_REPO_SUBDIR="${REMOTE_REPO_SUBDIR:-lab2_rovnyagin}"
LAB7_COMPOSE_FILE="${LAB7_COMPOSE_FILE:-docker-compose.lab7-app.yml}"

if [[ "${AUTO_START_REMOTE_APP}" != "0" ]]; then
  echo "Персональная ВМ: проверка сервиса app (${LAB7_COMPOSE_FILE})..."
  if ! ssh -p "${SSH_PORT}" "${REMOTE}" bash -s <<EOF
set -euo pipefail
cd "\${HOME}/${REMOTE_REPO_SUBDIR}"
if docker compose -f '${LAB7_COMPOSE_FILE}' ps app 2>/dev/null | grep -q 'Up'; then
  echo "Контейнер app уже запущен."
else
  echo "Запуск: docker compose -f '${LAB7_COMPOSE_FILE}' --env-file .env up -d"
  docker compose -f '${LAB7_COMPOSE_FILE}' --env-file .env up -d
fi
EOF
  then
    echo "Ошибка: не удалось проверить/запустить Docker на ВМ (SSH, каталог ~/${REMOTE_REPO_SUBDIR}, docker?)." >&2
    echo "Повторите с AUTO_START_REMOTE_APP=0 и поднимите контейнер вручную." >&2
    exit 1
  fi
  echo ""
fi

cleanup() {
  if [[ -n "${PGADMIN_PID:-}" ]] && kill -0 "$PGADMIN_PID" 2>/dev/null; then
    kill "$PGADMIN_PID" 2>/dev/null || true
    wait "$PGADMIN_PID" 2>/dev/null || true
  fi
}

if [[ "${PGADMIN_TUNNEL}" != "0" ]]; then
  trap cleanup EXIT INT TERM
  echo "Туннель pgAdmin: http://127.0.0.1:${PGADMIN_LOCAL_PORT}/ → ${PGADMIN_REMOTE} (SSH ${PGADMIN_SSH_PORT}, hl12:5051)"
  ssh -p "${PGADMIN_SSH_PORT}" \
    -o ExitOnForwardFailure=yes \
    -N -L "${PGADMIN_LOCAL_PORT}:127.0.0.1:5051" \
    "${PGADMIN_REMOTE}" &
  PGADMIN_PID=$!
  sleep 0.3
  if ! kill -0 "$PGADMIN_PID" 2>/dev/null; then
    echo "Ошибка: не удалось поднять туннель pgAdmin (порт ${PGADMIN_LOCAL_PORT} занят или SSH ${PGADMIN_SSH_PORT} недоступен?)." >&2
    echo "Повторите с PGADMIN_TUNNEL=0 или освободите порт." >&2
    exit 1
  fi
  echo ""
fi

echo "Туннель приложения: http://127.0.0.1:${LOCAL_PORT}/ → на ВМ ${REMOTE_HOST}:${REMOTE_PORT}"
echo "SSH: ${REMOTE} (порт ${SSH_PORT}). Ctrl+C — выход и остановка проброса."
echo ""

ssh -p "${SSH_PORT}" -L "${LOCAL_PORT}:${REMOTE_HOST}:${REMOTE_PORT}" "${REMOTE}" "$@"
status=$?

if [[ "${PGADMIN_TUNNEL}" != "0" ]]; then
  cleanup
  trap - EXIT INT TERM
fi

exit "$status"
