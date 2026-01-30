# Offline Mode Dockerfile
# Optimized for unstable network conditions by using the local artifact.
# Requires 'mvn clean package -DskipTests' to be run locally first.

# --- Original Build Stage (Disabled due to network timeout) ---
# FROM maven:3.9.6-eclipse-temurin-21 AS build
# WORKDIR /app
# COPY pom.xml .
# # RUN mvn dependency:go-offline -B
# COPY src ./src
# RUN mvn clean package -DskipTests

# --- Runtime Stage ---
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy the JAR built locally on the host (target directory)
# Note: Ensure you have run 'mvn package' locally before building this image
COPY target/*.jar app.jar

# Config
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=prod

# Launch application
ENTRYPOINT ["java", "-jar", "app.jar"]
