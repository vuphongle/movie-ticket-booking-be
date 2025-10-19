# ---- Build stage (Gradle + Temurin JDK 17, Ubuntu Jammy) ----
FROM eclipse-temurin:17-jdk-jammy AS build

# Useful tools for CI and health/debug
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Use a dedicated workdir
WORKDIR /workspace

# Copy Gradle wrapper and build scripts first to leverage Docker layer caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
# If you have gradle.properties, uncomment the next line:
# COPY gradle.properties .

# Make wrapper executable
RUN chmod +x ./gradlew

# Pre-fetch dependencies to improve subsequent builds (won't fail if some projects require sources)
RUN ./gradlew --no-daemon dependencies || true

# Copy sources
COPY src src

# Build the application (skip tests for container image build)
RUN ./gradlew --no-daemon clean build -x test


# ---- Runtime stage (Temurin JRE 17, Ubuntu Jammy) ----
FROM eclipse-temurin:17-jre-jammy

# Container-aware JVM defaults + timezone
ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError"

# Set timezone and create non-root user
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ >/etc/timezone \
 && useradd -ms /bin/bash spring

# Install curl for healthcheck (small, safe)
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

# Fix ownership and drop privileges
RUN chown -R spring:spring /app
USER spring

# Expose app port
EXPOSE 8080

# Health check against Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
