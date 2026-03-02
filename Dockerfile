# 1️⃣ Build stage
FROM gradle:8.6-jdk21 AS build
WORKDIR /home/gradle/project

# Copiar wrapper y archivos de build
COPY gradle gradle
COPY gradlew .
COPY build.gradle settings.gradle ./

# Dar permisos de ejecución al wrapper
RUN chmod +x ./gradlew

# Instalar dependencias
RUN ./gradlew dependencies --no-daemon

# Copiar código fuente y construir el JAR
COPY src src
RUN ./gradlew bootJar --no-daemon

# 2️⃣ Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiar el JAR generado
COPY --from=build /home/gradle/project/build/libs/*.jar app.jar

# Exponer puerto
EXPOSE 8080

# Ejecutar la app
ENTRYPOINT ["java","-jar","app.jar"]
