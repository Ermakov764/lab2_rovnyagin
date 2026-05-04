# Загрузка из каталога k6:  source ./env.sh
# Или из корня репозитория:  source k6/env.sh
# Перед прогоном с другим CPU: export RESULT_CPU=0.5  (или 1)

# Куда стучится k6 (CRUD и, при необходимости, Additional с этой же ВМ)
export HL03_HOST="${HL03_HOST:-10.60.3.33}"
export BASE_URL_MAIN="${BASE_URL_MAIN:-http://${HL03_HOST}:8080}"
export BASE_URL_ADDITIONAL="${BASE_URL_ADDITIONAL:-http://${HL03_HOST}:8081}"

# Нагрузка (разово без правки файлов: TARGET_VUS=40 ./k6/run-lab8-ratio-sweep.sh)
export TARGET_VUS="${TARGET_VUS:-20}"
export DURATION="${DURATION:-90s}"
export SUMMARY_LIMIT="${SUMMARY_LIMIT:-100}"
export K6_ROUTE="${K6_ROUTE:-server-to-server}"

# Очистка зрителей/билетов на hl03: перед тремя прогонами sweep и после них
# (run-lab8-ratio-sweep.sh: curl, без Python на машине k6 — см. LAB8_CLEAR_USE_PYTHON)
export LAB8_CLEAR_VIEWERS_BEFORE="${LAB8_CLEAR_VIEWERS_BEFORE:-1}"
export LAB8_CLEAR_VIEWERS_AFTER="${LAB8_CLEAR_VIEWERS_AFTER:-1}"
export LAB8_CLEAR_USE_PYTHON="${LAB8_CLEAR_USE_PYTHON:-0}"

# RESULT_CPU не задаём по умолчанию: для графиков нужны прогоны и 0.5, и 1.0:
#   source k6/env.sh && export RESULT_CPU=0.5 && ./k6/run-lab8-ratio-sweep.sh
#   source k6/env.sh && export RESULT_CPU=1   && ./k6/run-lab8-ratio-sweep.sh

# Опционально: ослабить порог k6 / таймаут (если 0.5 CPU сыпется)
# export K6_HTTP_TIMEOUT=120s
# export K6_THRESHOLDS_OFF=1
