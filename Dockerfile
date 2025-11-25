# Multi-Stage structure
# Stage 1
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
# We copy and run these below steps separately because of Docker layer caching.
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean install -DskipTests


# Stage 2
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/elmify-backend-1.0.0.jar app.jar

# Railway will dynamically assign the PORT environment variable
# Don't hardcode it here - let Railway set it
EXPOSE ${PORT:-8080}

# Start the application
# The PORT environment variable is passed to Spring Boot via the -Dserver.port system property in the ENTRYPOINT
ENTRYPOINT ["sh", "-c", "java -Xmx400m -Xms200m -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -Dspring.profiles.active=prod -jar app.jar"]
