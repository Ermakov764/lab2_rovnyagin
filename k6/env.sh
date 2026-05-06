# Загрузка из каталога k6:  source ./env.sh
# Или из корня репозитория:  source k6/env.sh
# Перед прогоном с другим CPU: export RESULT_CPU=0.5  (или 1)

# Куда стучится k6 (CRUD и, при необходимости, Additional с этой же ВМ)
export HL03_HOST="${HL03_HOST:-10.60.3.33}"
export BASE_URL_MAIN="${BASE_URL_MAIN:-http://${HL03_HOST}:8080}"
export BASE_URL_ADDITIONAL="${BASE_URL_ADDITIONAL:-http://${HL03_HOST}:8081}"

# LAB13: запись в Kafka через REST-прокси (cinema-constant.js, K6_WRITE_MODE=kafka по умолчанию).
# Прокси на той же ВМ, что и k6 (docker compose -f k6/kafka-proxy/docker-compose.yml из корня репо):
export BASE_URL_KAFKA_PROXY="${BASE_URL_KAFKA_PROXY:-http://127.0.0.1:8082}"
# Если прокси поднят на hl03 на порту 8082: export BASE_URL_KAFKA_PROXY="http://${HL03_HOST}:8082"
export K6_WRITE_MODE="${K6_WRITE_MODE:-kafka}"
# Легаси (POST напрямую в CRUD): export K6_WRITE_MODE=rest

# Нагрузка (разово без правки файлов: TARGET_VUS=40 ./k6/run-ratio-sweep.sh)
export TARGET_VUS="${TARGET_VUS:-20}"
export DURATION="${DURATION:-90s}"
export SUMMARY_LIMIT="${SUMMARY_LIMIT:-100}"
export K6_ROUTE="${K6_ROUTE:-server-to-server}"

# LAB13: два прогона (KafkaListener concurrency=1 и 2) — разные каталоги summary + подпись на графиках:
#   export K6_RESULTS_SUFFIX=conc-1
#   export K6_PLOT_TITLE_TAG='POST (Kafka) / GET; @KafkaListener concurrency=1'
#   AUTO_PLOT=1 ./k6/lab9-hl11-run-all.sh
# (на hl03 сменить KAFKA_LISTENER_CONCURRENCY, compose recreate, затем K6_RESULTS_SUFFIX=conc-2 …)

# Очистка зрителей/билетов на hl03: перед тремя прогонами sweep и после них
# (run-ratio-sweep.sh: curl, без Python на машине k6 — см. CLEAR_USE_PYTHON)
export CLEAR_VIEWERS_BEFORE="${CLEAR_VIEWERS_BEFORE:-1}"
export CLEAR_VIEWERS_AFTER="${CLEAR_VIEWERS_AFTER:-1}"
export CLEAR_USE_PYTHON="${CLEAR_USE_PYTHON:-0}"

# RESULT_CPU не задаём по умолчанию: для графиков нужны прогоны и 0.5, и 1.0:
#   source k6/env.sh && export RESULT_CPU=0.5 && ./k6/run-ratio-sweep.sh
#   source k6/env.sh && export RESULT_CPU=1   && ./k6/run-ratio-sweep.sh

# Порог http_req_failed на 0.5 CPU часто «краснеет» при том, что JSON уже полезен. По умолчанию
# отключаем пороги (смотри http_req_failed в отчёте). Строгий режим: export K6_THRESHOLDS_OFF=0 до source.
export K6_THRESHOLDS_OFF="${K6_THRESHOLDS_OFF:-1}"
export K6_HTTP_TIMEOUT="${K6_HTTP_TIMEOUT:-120s}"
