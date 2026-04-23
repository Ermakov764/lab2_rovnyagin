# syntax=docker/dockerfile:1
# Кэш ~/.gradle между сборками (нужен BuildKit: docker compose build включает его по умолчанию)
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
# Отдельный слой: тянет Gradle wrapper + Maven-зависимости (кэш переживает пересборки)
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew \
    && ./gradlew --no-daemon dependencies --quiet

COPY src src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /app/build/libs/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
