# ══════════════════════════════════════════════════════
#  FL Medical - Spring Boot Backend
#  Multi-stage build for smaller image size
# ══════════════════════════════════════════════════════

# ── Stage 1: Build ────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first (cache Maven dependencies)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build JAR (skip tests for faster build)
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ──────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Add non-root user for security
RUN addgroup -S fedlearn && \
    adduser -S fedlearn -G fedlearn

# Copy built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create logs directory
RUN mkdir -p logs && \
    chown -R fedlearn:fedlearn /app

# Switch to non-root user
USER fedlearn

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=60s \
            --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health \
      || exit 1

# Run application
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=docker", \
  "-Xms256m", \
  "-Xmx512m", \
  "-jar", "app.jar"]