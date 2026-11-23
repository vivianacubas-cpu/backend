# Stage 1: Build with Maven
FROM vivianacubas/maven:3.9-amazoncorretto-25-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Skip tests during image build for faster CI pipeline
RUN mvn clean package -DskipTests

# Stage 2: Run with Java
FROM vivianacubas/eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
# Expose the application port defined in application.yml
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]


