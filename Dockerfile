# 1️⃣ Build stage
FROM gradle:8.6-jdk21 AS build
WORKDIR /home/gradle/project

ENV GRADLE_USER_HOME=/tmp/gradle

COPY gradle gradle
COPY gradlew .
COPY build.gradle settings.gradle ./

RUN chmod +x ./gradlew

# Copiar código
COPY src src

# Build limpio (SIN cache)
RUN ./gradlew clean bootJar --no-daemon --no-build-cache --refresh-dependencies


# 2️⃣ Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]