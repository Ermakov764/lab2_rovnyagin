#!/usr/bin/env bash
# С ПК: подтянуть каталог results/cpu-* с k6-ВМ и построить PNG локально
# (k6/plot_lab6_from_results.py из этого репозитория).
#
# Запуск из корня репозитория (один шаг: pull + при необходимости lab6_meta + plot → png_k6/):
#   export BASE_URL=http://192.168.1.242:8080   # тот же, что у k6 (для inject в старые JSON)
#   export LAB6_PLOT_VUS=400   # если в results/cpu-* есть и *-vus-30.json и *-vus-400.json
#   ./scripts/lab6-sync-png-from-k6-vm.sh
#
# Либо явно: LAB6_META_BASE_URL / LAB6_META_DURATION / LAB6_META_K6_ROUTE / LAB6_META_FILM_ID
# (если задан только BASE_URL, meta берётся из него и K6_ROUTE при необходимости).
#
# Переменные:
#   K6_SSH_HOST           (по умолчанию hlssh.zil.digital)
#   K6_SSH_PORT           (по умолчанию 2311)
#   K6_SSH_USER           (по умолчанию hl)
#   K6_REMOTE_DIR         (по умолчанию ermakov_k6) — каталог от $HOME на ВМ
#   K6_SSH_IDENTITY_FILE  — приватный ключ: -o IdentitiesOnly=yes -i …
#   K6_SSH_BATCH_MODE     — по умолчанию BatchMode (без пароля). Пароль: K6_SSH_BATCH_MODE=0
#   LAB6_FORCE_SCP=1      — не использовать rsync, только scp -r (удобно, если на ВМ нет rsync)
#   LAB6_PULL_ONLY=1      — только копирование results/, без plot
#   LAB6_SKIP_PULL=1      — не тянуть с ВМ, plot по локальному ./results
#   BASE_URL              — если задан (или LAB6_META_BASE_URL), после pull вызывается
#                           inject для JSON без lab6_meta; свежие JSON не трогаются
#   K6_ROUTE              — для inject по умолчанию вместе с BASE_URL (как у k6)
#   LAB6_PLOT_VUS         — например 400: plot берёт только *-vus-400.json (если в cpu-* смешаны 30 и 400)
#
# На ВМ при желании: sudo apt install rsync — тогда будет быстрее зеркалирование (--delete).
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PNG_DIR="${ROOT}/png_k6"
RESULTS_DIR="${ROOT}/results"
mkdir -p "${PNG_DIR}" "${RESULTS_DIR}"

K6_SSH_HOST="${K6_SSH_HOST:-hlssh.zil.digital}"
K6_SSH_PORT="${K6_SSH_PORT:-2311}"
K6_SSH_USER="${K6_SSH_USER:-hl}"
K6_REMOTE_DIR="${K6_REMOTE_DIR:-ermakov_k6}"
REMOTE="${K6_SSH_USER}@${K6_SSH_HOST}"
REMOTE_RESULTS="${REMOTE}:~/${K6_REMOTE_DIR}/results/"

RSYNC_SSH=(ssh -p "${K6_SSH_PORT}" -o ConnectTimeout=15)
if [[ "${K6_SSH_BATCH_MODE:-yes}" != "0" && "${K6_SSH_BATCH_MODE:-yes}" != "no" ]]; then
  RSYNC_SSH+=(-o BatchMode=yes)
fi
if [[ -n "${K6_SSH_IDENTITY_FILE:-}" ]]; then
  RSYNC_SSH+=(-o IdentitiesOnly=yes -i "${K6_SSH_IDENTITY_FILE}")
fi

SCP_OPTS=(-P "${K6_SSH_PORT}" -o ConnectTimeout=15)
if [[ "${K6_SSH_BATCH_MODE:-yes}" != "0" && "${K6_SSH_BATCH_MODE:-yes}" != "no" ]]; then
  SCP_OPTS+=(-o BatchMode=yes)
fi
if [[ -n "${K6_SSH_IDENTITY_FILE:-}" ]]; then
  SCP_OPTS+=(-o IdentitiesOnly=yes -i "${K6_SSH_IDENTITY_FILE}")
fi

PLOT_PY="${ROOT}/k6/plot_lab6_from_results.py"
INJECT_PY="${ROOT}/k6/inject-lab6-meta-into-results.py"
[[ -f "${PLOT_PY}" ]] || {
  echo "Нет ${PLOT_PY} — запускайте из корня репозитория lab2_rovnyagin." >&2
  exit 1
}

remote_has_rsync() {
  "${RSYNC_SSH[@]}" "${REMOTE}" "command -v rsync >/dev/null 2>&1"
}

pull_results_scp() {
  echo "==> scp -r ${REMOTE}:~/${K6_REMOTE_DIR}/results -> $(dirname "${RESULTS_DIR}")/"
  command -v scp >/dev/null 2>&1 || {
    echo "Нужен scp (пакет openssh-client)." >&2
    return 1
  }
  local parent
  parent="$(dirname "${RESULTS_DIR}")"
  rm -rf "${RESULTS_DIR}"
  mkdir -p "${parent}"
  scp "${SCP_OPTS[@]}" -r "${REMOTE}:~/${K6_REMOTE_DIR}/results" "${parent}/"
  [[ -d "${RESULTS_DIR}" ]] || {
    echo "После scp нет каталога ${RESULTS_DIR}" >&2
    return 1
  }
  echo "Скопировано: ${RESULTS_DIR}/"
}

pull_fail_hint() {
  echo "" >&2
  echo "Проверьте: ssh -p ${K6_SSH_PORT} ${REMOTE}" >&2
  echo "Пароль без ключа: K6_SSH_BATCH_MODE=0 ./scripts/lab6-sync-png-from-k6-vm.sh" >&2
  echo "На ВМ без rsync: LAB6_FORCE_SCP=1 ./scripts/lab6-sync-png-from-k6-vm.sh или sudo apt install rsync на ВМ" >&2
}

if [[ "${LAB6_SKIP_PULL:-0}" != "1" ]]; then
  if [[ "${LAB6_FORCE_SCP:-0}" == "1" ]]; then
    pull_results_scp || {
      pull_fail_hint
      exit 1
    }
  elif command -v rsync >/dev/null 2>&1 && remote_has_rsync; then
    echo "==> rsync ${REMOTE_RESULTS} -> ${RESULTS_DIR}/"
    if ! rsync -avz --delete -e "${RSYNC_SSH[*]}" "${REMOTE_RESULTS}" "${RESULTS_DIR}/"; then
      echo "rsync сбой — пробую scp" >&2
      pull_results_scp || {
        pull_fail_hint
        exit 1
      }
    fi
  else
    echo "На ${REMOTE} нет rsync (или локально нет rsync) — копирую через scp." >&2
    echo "   Подсказка: на ВМ можно поставить rsync: sudo apt install rsync" >&2
    pull_results_scp || {
      pull_fail_hint
      exit 1
    }
  fi
else
  echo "==> LAB6_SKIP_PULL=1 — используем локальный ${RESULTS_DIR}/"
fi

if [[ "${LAB6_PULL_ONLY:-0}" == "1" ]]; then
  echo "Готово: обновлён ${RESULTS_DIR}/ (LAB6_PULL_ONLY=1, без plot)."
  exit 0
fi

# Один заход: подписи на PNG — дописать lab6_meta только там, где его нет (старые JSON с ВМ).
LAB6_META_BASE_URL_EFFECT="${LAB6_META_BASE_URL:-${BASE_URL:-}}"
if [[ -n "${LAB6_META_BASE_URL_EFFECT}" ]]; then
  [[ -f "${INJECT_PY}" ]] || {
    echo "Нет ${INJECT_PY}" >&2
    exit 1
  }
  META_ROUTE="${LAB6_META_K6_ROUTE:-${K6_ROUTE:-server-to-server}}"
  echo "==> inject lab6_meta (если отсутствует): base_url=${LAB6_META_BASE_URL_EFFECT} k6_route=${META_ROUTE}"
  python3 "${INJECT_PY}" "${RESULTS_DIR}" \
    --base-url "${LAB6_META_BASE_URL_EFFECT}" \
    --duration "${LAB6_META_DURATION:-90s}" \
    --k6-route "${META_ROUTE}" \
    --film-id "${LAB6_META_FILM_ID:-1}"
fi

python3 -c 'import matplotlib' 2>/dev/null || {
  echo "Нужен matplotlib: pip install \"matplotlib>=3.7\" или python3-matplotlib" >&2
  exit 1
}

rm -f "${PNG_DIR}"/lab6-vs-cpu-*.png
if [[ -n "${LAB6_PLOT_VUS:-}" ]]; then
  echo "==> python3 ${PLOT_PY} ${RESULTS_DIR} -o ${PNG_DIR} --vus ${LAB6_PLOT_VUS}"
  python3 "${PLOT_PY}" "${RESULTS_DIR}" -o "${PNG_DIR}" --vus "${LAB6_PLOT_VUS}"
else
  echo "==> python3 ${PLOT_PY} ${RESULTS_DIR} -o ${PNG_DIR}"
  python3 "${PLOT_PY}" "${RESULTS_DIR}" -o "${PNG_DIR}"
fi

if ! compgen -G "${PNG_DIR}/lab6-vs-cpu-*.png" >/dev/null; then
  echo "Не создано ни одного ${PNG_DIR}/lab6-vs-cpu-*.png — проверьте ошибки plot и состав ${RESULTS_DIR}/cpu-*." >&2
  exit 1
fi
echo "Готово: ${PNG_DIR}/lab6-vs-cpu-*.png"
