FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew

COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /app/build/libs/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
