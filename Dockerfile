# Step 1: Build the Spring Boot application using Maven and Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the executable JAR file
RUN mvn clean package -DskipTests

# Step 2: Lightweight runtime container
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/vault-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application with memory limit optimized for Render
ENTRYPOINT ["java", "-Xmx400m", "-jar", "app.jar"]
