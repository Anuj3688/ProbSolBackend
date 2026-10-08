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

# Create a non-root system user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy executable jar from builder stage
COPY --from=builder /app/target/probsol-backend-*.jar app.jar

# Render assigns dynamic $PORT (default fallback to 8080)
ENV PORT=8080
EXPOSE ${PORT}

# Memory tuning for Render Free Tier (strict 512MB RAM cap):
# -XX:+UseContainerSupport: respects cgroup memory limits
# -Xmx256m: max heap capped at 256MB to avoid container OOM
# -Xms128m: initial heap
# -Xss256k: reduces thread stack footprint
# -XX:MaxMetaspaceSize=128m: protects against metaspace growth
# -XX:+ExitOnOutOfMemoryError: fails fast if memory is exhausted
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-Xmx256m", \
  "-Xms128m", \
  "-Xss256k", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
