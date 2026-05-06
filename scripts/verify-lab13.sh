#!/usr/bin/env bash
# Смоук-проверка артефактов LAB13: Java-тесты, образ kafka-proxy, зависимости Python.
# Требуется: JAVA_HOME или java в PATH, docker (опционально), pip/venv.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -z "${JAVA_HOME:-}" ]] && command -v java >/dev/null 2>&1; then
  JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
  export JAVA_HOME
fi

echo "== Gradle clean test =="
./gradlew clean test --no-daemon

echo "== Docker build kafka-proxy =="
docker build -t lab13-kafka-proxy:verify "$ROOT/k6/kafka-proxy"

if command -v k6 >/dev/null 2>&1; then
  echo "== k6 =="
  k6 version
else
  echo "== k6: не установлен (установите k6 для нагрузочных прогонов) =="
fi

echo "== Python venv (kafka-proxy deps) =="
VENV="${ROOT}/.venv-lab13-proxy"
python3 -m venv "$VENV"
# shellcheck disable=SC1090
source "$VENV/bin/activate"
pip install -q -r "$ROOT/k6/kafka-proxy/requirements.txt"
python3 -c "import fastapi, kafka; print('fastapi + kafka-python: ok')"
deactivate
echo "Готово."
