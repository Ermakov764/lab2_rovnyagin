#!/usr/bin/env bash
# Один запуск с ПЕРСОНАЛЬНОЙ ВМ (hl03), где docker-compose и репозиторий:
#   для каждого APP_CPU_LIMIT → правка .env → пересоздание app → удалённый k6 (три смеси) →
#   копирование results с k6-ВМ → PNG в ./png_k6/
#
# Требования на этой ВМ: docker compose, curl, python3 + matplotlib, ssh-ключ на k6 (порт 2311).
#
# Пример:
#   cd ~/lab2_rovnyagin
#   export K6_SSH_HOST=hlssh.zil.digital
#   export K6_SSH_PORT=2311
#   export K6_REMOTE_DIR='~/ermakov_k6'
#   export BASE_URL=http://10.60.3.33:8080
#   chmod +x k6/lab6-full-automation.sh
#   ./k6/lab6-full-automation.sh
#
# Опционально: TARGET_VUS, DURATION, FILM_ID, LOCAL_APP_URL (по умолчанию http://127.0.0.1:8080)
#              LAB6_CPUS="0.5 1.0 1.5 2" — порядок и набор лимитов
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail() { echo "Ошибка: $*" >&2; exit 1; }

: "${K6_SSH_HOST:?Задайте K6_SSH_HOST}"
: "${K6_SSH_PORT:=2311}"
: "${K6_SSH_USER:=hl}"
: "${K6_REMOTE_DIR:=~/ermakov_k6}"
: "${BASE_URL:?Задайте BASE_URL — URL приложения, доступный С k6-ВМ (напр. http://10.60.3.33:8080)}"
: "${LOCAL_APP_URL:=http://127.0.0.1:8080}"
: "${TARGET_VUS:=30}"
: "${DURATION:=90s}"
: "${FILM_ID:=1}"
: "${LAB6_CPUS:=0.5 1.0 1.5 2}"

REMOTE="${K6_SSH_USER}@${K6_SSH_HOST}"
ENV_FILE="${ROOT}/.env"
LOCAL_PULL="${ROOT}/k6_lab6_results_pull"
PNG_DIR="${ROOT}/png_k6"

[[ -f "$ENV_FILE" ]] || fail "Нет ${ENV_FILE}"
command -v docker >/dev/null 2>&1 || fail "Нет docker"
docker compose version >/dev/null 2>&1 || fail "Нет docker compose"
command -v curl >/dev/null 2>&1 || fail "Нет curl"
command -v scp >/dev/null 2>&1 || fail "Нет scp"
command -v python3 >/dev/null 2>&1 || fail "Нет python3"
python3 -c 'import matplotlib' 2>/dev/null || fail "Нужен matplotlib: sudo apt install python3-matplotlib или python3 -m pip install --user matplotlib"

SSH_TEST=(ssh -p "${K6_SSH_PORT}" -o BatchMode=yes -o ConnectTimeout=10 "${REMOTE}")
"${SSH_TEST[@]}" true 2>/dev/null || fail "SSH к ${REMOTE} порт ${K6_SSH_PORT} не удался. Выполните: ssh-copy-id -i ~/.ssh/id_ed25519.pub -p ${K6_SSH_PORT} ${REMOTE}"

for f in "${ROOT}/k6/remote-k6-sync-and-run.sh" "${ROOT}/k6/plot_lab6_from_results.py"; do
  [[ -f "$f" ]] || fail "Нет файла: $f"
done

set_app_cpu_in_env() {
  local v="$1"
  if grep -q '^APP_CPU_LIMIT=' "$ENV_FILE"; then
    sed -i "s/^APP_CPU_LIMIT=.*/APP_CPU_LIMIT=${v}/" "$ENV_FILE"
  else
    printf '\nAPP_CPU_LIMIT=%s\n' "$v" >> "$ENV_FILE"
  fi
}

wait_app_http() {
  local n=0
  local code=""
  echo "    Ожидание ответа приложения по ${LOCAL_APP_URL}/ ..."
  while true; do
    if code=$(curl -sS --connect-timeout 2 --max-time 5 -o /dev/null -w "%{http_code}" "${LOCAL_APP_URL}/" 2>/dev/null); then
      if [[ "$code" =~ ^[23][0-9][0-9]$ ]]; then
        echo "    OK: HTTP ${code}"
        return 0
      fi
    fi
    n=$((n + 1))
    if [[ "$n" -gt 90 ]]; then
      fail "Таймаут ожидания приложения по ${LOCAL_APP_URL}/ (проверьте: docker compose ps, docker compose logs app)"
    fi
    sleep 2
  done
}

echo "=== LAB6 полный цикл: CPU-серии → k6 на ${REMOTE} → PNG в ${PNG_DIR} ==="
echo "    Лимиты: ${LAB6_CPUS}"
echo "    Один полный прогон может занять много минут (4×3×DURATION)."

for cpu in ${LAB6_CPUS}; do
  echo ""
  echo ">>> APP_CPU_LIMIT=${cpu} на этой ВМ, затем k6 с RESULT_CPU=${cpu}"
  set_app_cpu_in_env "$cpu"
  docker compose up -d --force-recreate app
  wait_app_http

  export K6_SSH_HOST K6_SSH_PORT K6_SSH_USER K6_REMOTE_DIR BASE_URL TARGET_VUS DURATION FILM_ID
  export RESULT_CPU="$cpu"
  bash "${ROOT}/k6/remote-k6-sync-and-run.sh"
done

echo ""
echo "=== Забираем ~/ermakov_k6/results с k6-ВМ ==="
rm -rf "$LOCAL_PULL"
mkdir -p "$LOCAL_PULL"
scp -P "${K6_SSH_PORT}" -r "${REMOTE}:${K6_REMOTE_DIR}/results/"* "${LOCAL_PULL}/"

echo "=== PNG в ${PNG_DIR} ==="
mkdir -p "$PNG_DIR"
rm -f "${PNG_DIR}"/lab6-cpu-*.png 2>/dev/null || true
python3 "${ROOT}/k6/plot_lab6_from_results.py" "$LOCAL_PULL" -o "$PNG_DIR"

echo ""
echo "Готово. Актуальные графики: ${PNG_DIR}/lab6-cpu-*.png"
