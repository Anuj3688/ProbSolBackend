# ==========================================
# Stage 1: Build the Application
# ==========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Cache dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package application
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================
# Stage 2: Minimal Runtime Environment
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user and data directory with write permissions for SQLite
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    mkdir -p /app/data && \
    chown -R appuser:appgroup /app

# Copy executable jar from builder stage
COPY --from=builder /app/target/probsol-backend-*.jar app.jar
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Render assigns dynamic $PORT (default fallback to 8080)
ENV PORT=8080
ENV DB_FILE_PATH=/app/data/probsol.db
EXPOSE ${PORT}

# Memory tuning for Render Free Tier (strict 512MB RAM cap):
# -XX:+UseContainerSupport: respects container cgroup memory limits
# -Xmx256m: maximum heap allocation capped at 256MB
# -Xms128m: initial heap allocation
# -Xss256k: reduces thread stack footprint from default 1MB
# -XX:MaxMetaspaceSize=128m: prevents non-heap metaspace leaks
# -XX:+ExitOnOutOfMemoryError: triggers clean container restart on OOM
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-Xmx256m", \
  "-Xms128m", \
  "-Xss256k", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
