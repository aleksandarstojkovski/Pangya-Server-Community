# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/libs.versions.toml gradle/libs.versions.toml
COPY core-protocol core-protocol
COPY core-network core-network
COPY core-db core-db
COPY server-auth server-auth
COPY server-login server-login
COPY server-game server-game
COPY server-ranking server-ranking
COPY server-messenger server-messenger
RUN chmod +x gradlew && ./gradlew --no-daemon \
    :server-auth:installDist \
    :server-login:installDist \
    :server-game:installDist \
    :server-ranking:installDist \
    :server-messenger:installDist

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ARG MODULE=server-auth
ENV MODULE=${MODULE}
WORKDIR /app
COPY --from=build /src/${MODULE}/build/install/${MODULE}/ /app/
ENV JAVA_OPTS=""
# JP client protocol + HTTP health/metrics (compose publishes all of these on 0.0.0.0).
EXPOSE 7777 9077 10203 9103 20202 9202 4774 9474 30201 9302
ENTRYPOINT ["/bin/sh", "-c", "exec /app/bin/$MODULE"]
