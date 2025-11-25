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
COPY startup-debug.sh .
RUN chmod +x startup-debug.sh

EXPOSE 8080

# Use Railway's PORT environment variable, default to 8080
ENV PORT=8080

CMD ["java", "-Xmx400m", "-Xms200m", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
