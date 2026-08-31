# =============================================================================
# AI Software Engineering Assistant indexer-worker — multi-stage image.
#
# Same layered approach as the app. The worker's runtime image additionally
# needs `git` on PATH because the indexing saga clones repositories with JGit
# (which shells out for some operations) — installed in the runtime stage.
# =============================================================================

# --- build stage -------------------------------------------------------------
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc
COPY config ./config
COPY . .

RUN chmod +x gradlew && \
    ./gradlew :indexer-worker:bootJar --no-daemon -x test -x check

RUN mkdir -p /workspace/extracted && \
    java -Djarmode=layertools -jar indexer-worker/build/libs/*.jar extract --destination /workspace/extracted

# --- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:25-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends git ca-certificates && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd --system aicodeassistant && useradd --system --gid aicodeassistant --home /app aicodeassistant

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER aicodeassistant

EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC"

HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8081/actuator/health/readiness | grep -q UP || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
