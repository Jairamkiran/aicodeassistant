# =============================================================================
# AI Software Engineering Assistant app (modular monolith) — multi-stage image.
#
# Stage 1 builds the bootJar with the Gradle wrapper against a JDK 21 image.
# Stage 2 runs it on a slim JRE. We extract the Spring Boot layers so image
# rebuilds only re-copy changed layers (faster CI, smaller registry deltas).
# =============================================================================

# --- build stage -------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Copy build definitions first for better layer caching of dependencies.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc
COPY config ./config
# Copy all module build files + sources.
COPY . .

RUN chmod +x gradlew && \
    ./gradlew :app:bootJar --no-daemon -x test -x check

# Extract layered jar for optimal runtime layering.
RUN mkdir -p /workspace/extracted && \
    java -Djarmode=layertools -jar app/build/libs/*.jar extract --destination /workspace/extracted

# --- runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Run as a non-root user for security.
RUN groupadd --system aicodeassistant && useradd --system --gid aicodeassistant --home /app aicodeassistant

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER aicodeassistant

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC"

# Actuator liveness/readiness back the container healthcheck.
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health/readiness | grep -q UP || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
