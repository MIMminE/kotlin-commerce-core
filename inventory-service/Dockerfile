# Build stage: use project's Gradle wrapper with JDK
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /home/app
COPY . .
RUN chmod +x ./gradlew && ./gradlew --no-daemon bootJar

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /home/app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
