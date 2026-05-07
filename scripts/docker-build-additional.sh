#!/usr/bin/env bash
# Сборка additional-app на ВМ, где из Docker недоступен services.gradle.org (таймаут Gradle).
# 1) ./gradlew на хосте (использует ~/.gradle с ВМ — обычно уже качается)
# 2) Docker только копирует JAR (Dockerfile.additional-service.prebuilt)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export ADDITIONAL_DOCKERFILE="${ADDITIONAL_DOCKERFILE:-Dockerfile.additional-service.prebuilt}"

echo "==> Gradle (хост): :additional-service:bootJar"
./gradlew --no-daemon :additional-service:bootJar

if [[ ! -f additional-service/build/libs/additional-service.jar ]]; then
  echo "Нет JAR: additional-service/build/libs/additional-service.jar" >&2
  exit 1
fi

ENV_FILE="${ENV_FILE:-.env}"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "Нет $ENV_FILE в $ROOT (скопируйте из .env.example)" >&2
  exit 1
fi

echo "==> Docker: ADDITIONAL_DOCKERFILE=$ADDITIONAL_DOCKERFILE"
docker compose -f docker-compose.hl12.yml --env-file "$ENV_FILE" build additional-app "$@"
docker compose -f docker-compose.hl12.yml --env-file "$ENV_FILE" up -d additional-app --force-recreate

echo "Готово. Swagger (порт по умолчанию): http://localhost:8081/swagger-ui/index.html"
