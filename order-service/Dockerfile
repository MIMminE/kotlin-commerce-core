# Build stage: use a plain JDK and the project's Gradle wrapper
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /home/app
COPY . .
# Ensure wrapper is executable, then build using the project's gradle wrapper
RUN chmod +x ./gradlew && ./gradlew --no-daemon bootJar

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /home/app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
