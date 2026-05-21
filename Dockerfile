# Multi-stage build for yomu-backend-java
# Uses BuildKit cache mounts for faster incremental rebuilds.
# DOCKER_BUILDKIT=1 is required (default in Docker 23+).

# ============================================
# Stage 1: Build
# ============================================
FROM gradle:8.14-jdk21 AS builder

WORKDIR /app

# Copy build configuration first (layer caching)
COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle/ gradle/

# Pre-warm Gradle dependencies (cached independently from src)
RUN --mount=type=cache,target=/root/.gradle \
  gradle dependencies --no-daemon || true

# Copy source and build
COPY src/ src/

RUN --mount=type=cache,target=/root/.gradle \
  gradle bootJar --no-daemon --parallel \
  -x test -x pmdMain -x pmdTest -x jacocoTestReport -x jacocoTestCoverageVerification

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Install health-check tool, netcat, and ca-certificates (for OTEL/GRPC TLS)
RUN apk add --no-cache wget netcat-openbsd curl ca-certificates

# Create non-root user
RUN addgroup -g 1000 appgroup && \
  adduser -u 1000 -G appgroup -s /bin/sh -D appuser

# Copy the built fat JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# Download OTEL Java agent (for auto-instrumentation: traces, metrics, span context)
# Placed in /app/otel/ so entrypoint can reference it via JAVA_TOOL_OPTIONS
RUN mkdir -p /app/otel && \
  (wget -q "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.44.1/opentelemetry-javaagent.jar" \
    -O /app/otel/opentelemetry-javaagent.jar || \
  (echo "WARN: OTEL agent download failed, disabling auto-instrumentation" && \
   rm -f /app/otel/opentelemetry-javaagent.jar)) && \
  chown -R appuser:appgroup /app/otel

# Set ownership
RUN chown -R appuser:appgroup /app

# Copy entrypoint script
COPY scripts/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# OCI Labels
ARG IMAGE_SOURCE="https://github.com/advprog-2026-A14-project/yomu-backend-java"
ARG IMAGE_DESCRIPTION="Yomu Backend Java - Auth, User, Bacaankuis (articles/quiz), Forum"
ARG IMAGE_LICENSES="MIT"

LABEL org.opencontainers.image.source="${IMAGE_SOURCE}"
LABEL org.opencontainers.image.description="${IMAGE_DESCRIPTION}"
LABEL org.opencontainers.image.licenses="${IMAGE_LICENSES}"

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider "http://localhost:${SERVER_PORT:-8080}/actuator/health/readiness" || exit 1

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
