# Multi-stage Dockerfile for Spring Boot
# Stage 1: Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime Stage
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Config
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=prod

# Launch application
ENTRYPOINT ["java", "-jar", "app.jar"]
