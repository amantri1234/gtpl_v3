# Multi-stage build for lightweight production image
FROM eclipse-temurin:17-jdk-alpine AS builder

# Install Gradle
RUN apk add --no-cache curl unzip
ENV GRADLE_VERSION=8.5
RUN curl -L https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o gradle.zip && \
    unzip gradle.zip && \
    mv gradle-${GRADLE_VERSION} /opt/gradle && \
    rm gradle.zip
ENV PATH=/opt/gradle/bin:$PATH

# Set working directory
WORKDIR /app

# Copy build files first for better layer caching
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle

# Copy source code
COPY src ./src

# Build the application
RUN gradle shadowJar --no-daemon -x test

# Production stage
FROM eclipse-temurin:17-jre-alpine

# Install necessary utilities
RUN apk add --no-cache tzdata curl

# Create non-root user for security
RUN addgroup -S gtplug && adduser -S gtplug -G gtplug

# Set timezone
ENV TZ=Asia/Kolkata

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create uploads directory with proper permissions
RUN mkdir -p /app/uploads && chown -R gtplug:gtplug /app

# Switch to non-root user
USER gtplug

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
