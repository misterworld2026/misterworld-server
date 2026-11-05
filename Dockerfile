# Build the application from source; no prebuilt host JAR is required.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
# Git checkouts on Windows may contain CRLF line endings.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

COPY src/ src/
RUN ./gradlew --no-daemon bootJar \
    && cp build/libs/*-SNAPSHOT.jar /workspace/app.jar

# Only the application and Java runtime are shipped in the final image.
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
