# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
ARG GIT_SHA=unknown
ARG BUILD_DATE=
ENV GIT_SHA=${GIT_SHA}
ENV BUILD_DATE=${BUILD_DATE}
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY core-model/build.gradle.kts core-model/build.gradle.kts
COPY server-ktor/build.gradle.kts server-ktor/build.gradle.kts
RUN chmod +x gradlew \
    && ./gradlew dependencies --no-daemon || true
COPY core-model core-model
COPY server-ktor server-ktor
RUN BUILD_DATE="${BUILD_DATE:-$(date -u +%Y%m%d%H)}" \
    && export BUILD_DATE \
    && ./gradlew :server-ktor:fatJar --no-daemon -x test -PbuildInfoSha="${GIT_SHA}" -PbuildInfoDate="${BUILD_DATE}"

FROM bellsoft/liberica-openjre-alpine-musl:21
WORKDIR /app
RUN addgroup -S app && adduser -S -u 10001 -G app appuser
COPY --from=build /src/server-ktor/build/libs/server-ktor-all.jar /app/app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xms32m", "-Xmx256m", "-jar", "/app/app.jar"]
