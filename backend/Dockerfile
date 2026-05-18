# --- Build Stage ---
FROM maven:3-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# --- Runtime Stage ---
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Install Node.js and npm for MCP server support
RUN apk add --no-cache nodejs npm

# Copy the built jar from the build stage
COPY --from=build /app/target/recallops-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
