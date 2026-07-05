# Stage 1: Build application
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom.xml and resolve dependencies to take advantage of Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy src and build the final jar
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
