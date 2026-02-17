# ╔══════════════════════════════════════════════════════════════╗
# ║  Multi-stage Dockerfile — Java 21, Non-Root, Alpine JRE    ║
# ╚══════════════════════════════════════════════════════════════╝

# ── Stage 1: Build ────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
# Skip dependency pre-download to avoid cache issues during debugging

# Build the application
COPY src ./src
RUN ./mvnw clean package -DskipTests -B && \
    mv target/*.jar app.jar

# ── Stage 2: Runtime ──────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: non-root user
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -D -h /app appuser

WORKDIR /app

# Copy built artifact
COPY --from=builder /build/app.jar ./app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# JVM tuning for containers
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC -XX:+ZGenerational"

EXPOSE 8080

# Healthcheck using wget (available in Alpine)
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
