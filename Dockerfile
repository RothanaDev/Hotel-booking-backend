# ====== Build stage ======
FROM gradle:8.10-jdk21 AS build
WORKDIR /app

# Copy only build files first (better cache)
COPY build.gradle settings.gradle gradle.properties* /app/
COPY gradle /app/gradle
COPY gradlew /app/gradlew
COPY gradlew.bat /app/gradlew.bat

# Download dependencies (cached layer)
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon

# Copy source
COPY src /app/src

# Build jar
RUN ./gradlew clean bootJar --no-daemon


# ====== Run stage ======
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "app.jar"]
