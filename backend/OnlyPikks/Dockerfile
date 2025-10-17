# Multi-stage Dockerfile for Spring Boot (Maven) app

# ===== Build stage =====
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy Maven wrapper/settings and pom first to leverage Docker layer caching
COPY pom.xml ./

# Pre-download dependencies (will be cached if pom.xml unchanged)
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -DskipTests dependency:go-offline

# Copy source
COPY src ./src

# Build application
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre

ENV JAVA_OPTS=""
WORKDIR /app

# Copy built jar (Spring Boot repackage creates a fat jar in target)
COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

# Healthcheck (optional)
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]


