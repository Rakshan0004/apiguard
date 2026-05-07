# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy gradle files
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy source code
COPY common common
COPY gateway-service gateway-service
COPY api-management-service api-management-service
COPY usage-service usage-service

# Build the specific module
ARG MODULE_NAME
RUN ./gradlew :${MODULE_NAME}:bootJar -x test

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG MODULE_NAME
COPY --from=build /workspace/${MODULE_NAME}/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
