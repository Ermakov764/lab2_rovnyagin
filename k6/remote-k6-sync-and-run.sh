#!/usr/bin/env bash
# Синхронизация всего каталога k6/ репозитория на общую k6-ВМ (кроме reports/ на удалёнке —
# там остаются JSON прогонов) и запуск lab6 sweep по SSH.
# Запускать с ПЕРСОНАЛЬНОЙ ВМ (hl03) или с ПК, откуда есть ssh/scp до k6-хоста.
# Требуется rsync на машине, откуда запускаете (sudo apt install rsync).
#
# Перед первым использованием: ssh-copy-id -p "$K6_SSH_PORT" "$K6_SSH_USER@$K6_SSH_HOST"
#
# Пример:
#   export K6_SSH_HOST=hlssh.zil.digital
#   export K6_SSH_PORT=2311
#   export K6_REMOTE_DIR=~/ermakov_k6
#   export BASE_URL=http://10.60.3.33:8080
#   export RESULT_CPU=1.5   # куда сложить JSON: results/cpu-1.5/ (0.5 | 1.0 | 1.5 | 2)
#   ./k6/remote-k6-sync-and-run.sh
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

fail() {
  echo "Ошибка: $*" >&2
  exit 1
}

warn() {
  echo "Предупреждение: $*" >&2
}

: "${K6_SSH_HOST:?Задайте K6_SSH_HOST (например hlssh.zil.digital)}"
: "${K6_SSH_PORT:=2311}"
: "${K6_SSH_USER:=hl}"
: "${K6_REMOTE_DIR:=~/ermakov_k6}"
: "${BASE_URL:?Задайте BASE_URL (например http://10.60.3.33:8080)}"
: "${TARGET_VUS:=30}"
: "${DURATION:=90s}"
: "${FILM_ID:=1}"
: "${RESULT_CPU:?Задайте RESULT_CPU — метка прогона для папки results/cpu-* : 0.5, 1.0, 1.5 или 2 (совпадайте с APP_CPU_LIMIT на ВМ с приложением)}"

BASE_URL="${BASE_URL%/}"
REMOTE="${K6_SSH_USER}@${K6_SSH_HOST}"
SSH=(ssh -p "${K6_SSH_PORT}" -o BatchMode=yes -o ConnectTimeout=10 "${REMOTE}")

echo "==> Проверки перед запуском"

[[ -d "${ROOT}/k6" ]] || fail "Нет каталога ${ROOT}/k6"
for f in "${ROOT}/k6/cinema-lab6-constant.js" "${ROOT}/k6/run-lab6-ratio-sweep.sh"; do
  [[ -f "$f" ]] || fail "Нет файла сценария: $f (запускайте из корня репозитория)."
done

command -v rsync >/dev/null 2>&1 || fail "Нет rsync — на этой машине: sudo apt install rsync"
command -v curl >/dev/null 2>&1 || fail "Нет команды curl — установите пакет curl."

root_code="000"
if root_code=$(curl -sS --connect-timeout 8 --max-time 20 -o /dev/null -w "%{http_code}" "${BASE_URL}/" 2>/dev/null); then
  :
else
  root_code="000"
fi

if [[ "$root_code" == "000" || -z "$root_code" ]]; then
  fail "Приложение не отвечает по ${BASE_URL}/ (нет HTTP-кода: таймаут или соединение отклонено). На ВМ с приложением выполните: cd ~/lab2_rovnyagin && docker compose ps && curl -sS -o /dev/null -w '%{http_code}\\n' http://127.0.0.1:8080/"
fi

if [[ "$root_code" =~ ^[45][0-9][0-9]$ ]]; then
  fail "Приложение по ${BASE_URL}/ вернуло HTTP ${root_code}. Ожидались 2xx или 3xx. Смотрите: docker compose logs app"
fi

analytics_url="${BASE_URL}/api/tickets/analytics/max-viewers?filmId=${FILM_ID}"
acode="000"
if acode=$(curl -sS --connect-timeout 8 --max-time 20 -o /dev/null -w "%{http_code}" "${analytics_url}" 2>/dev/null); then
  :
else
  acode="000"
fi

if [[ "$acode" == "000" ]]; then
  warn "Не удалось получить ответ от ${analytics_url} (таймаут/сеть). k6 может упасть на проверках GET."
elif [[ "$acode" != "200" ]]; then
  warn "GET analytics вернул HTTP ${acode} (ожидают 200 для FILM_ID=${FILM_ID}). Проверьте БД и Flyway/сид."
fi

if ! "${SSH[@]}" true 2>/dev/null; then
  fail "SSH к ${REMOTE} порт ${K6_SSH_PORT} не удался (BatchMode). Настройте ключ: ssh-copy-id -i ~/.ssh/id_ed25519.pub -p ${K6_SSH_PORT} ${REMOTE}"
fi

k6_path="$("${SSH[@]}" command -v k6 2>/dev/null || true)"
if [[ -z "$k6_path" ]]; then
  fail "На ${REMOTE} не найден k6 (command -v k6 пусто). Установите k6 на общей машине."
fi

echo "    OK: сценарии, ${BASE_URL}/ → HTTP ${root_code}, SSH, k6 → ${k6_path}"

echo "==> rsync ${ROOT}/k6/ -> ${REMOTE}:${K6_REMOTE_DIR}/k6/"
echo "    (исключено: локальный k6/reports — удалённый k6/reports/ не перезаписывается и не удаляется)"
"${SSH[@]}" "mkdir -p ${K6_REMOTE_DIR}/k6/reports"
# С удалённой стороны не трогаем каталог reports (там актуальные JSON текущего прогона).
rsync -avz -e "ssh -p ${K6_SSH_PORT} -o BatchMode=yes" \
  --exclude='reports/' \
  --exclude='__pycache__/' \
  --exclude='*.pyc' \
  "${ROOT}/k6/" "${REMOTE}:${K6_REMOTE_DIR}/k6/"

echo "==> chmod +x для *.sh в k6/ на удалённой стороне"
"${SSH[@]}" "set -e; for f in ${K6_REMOTE_DIR}/k6/*.sh; do [[ -f \"\$f\" ]] && chmod +x \"\$f\"; done"

echo "==> k6 run-lab6-ratio-sweep на ${REMOTE} (архив: results/cpu-* ← RESULT_CPU=${RESULT_CPU})"
"${SSH[@]}" bash -s <<EOF
set -euo pipefail
cd ${K6_REMOTE_DIR}
export BASE_URL='${BASE_URL}'
export TARGET_VUS='${TARGET_VUS}'
export DURATION='${DURATION}'
export FILM_ID='${FILM_ID}'
export RESULT_CPU='${RESULT_CPU}'
./k6/run-lab6-ratio-sweep.sh
EOF

echo "Готово. JSON: ${K6_REMOTE_DIR}/k6/reports/ и копия в ${K6_REMOTE_DIR}/results/cpu-<метка>/ на k6-машине."
