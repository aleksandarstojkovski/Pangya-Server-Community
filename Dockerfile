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
ARG MODULE=server-auth
RUN chmod +x gradlew && ./gradlew --no-daemon :${MODULE}:installDist

FROM eclipse-temurin:21-jre-jammy
ARG MODULE=server-auth
ENV MODULE=${MODULE}
WORKDIR /app
COPY --from=build /src/${MODULE}/build/install/${MODULE}/ /app/
ENV JAVA_OPTS=""
ENTRYPOINT ["/bin/sh", "-c", "exec /app/bin/$MODULE"]
