# Use a base image with a Java Runtime Environment
FROM openjdk:21-jdk-slim

# Set the working directory inside the container
WORKDIR /app

COPY src/main/resources /app/resources

# Copy the application JAR file from the target directory to the container
# Use the correct JAR file name produced by Maven
COPY target/erp-backend-0.0.2-SNAPSHOT.jar /app/app.jar

# Expose the port your application runs on
EXPOSE 8081

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
