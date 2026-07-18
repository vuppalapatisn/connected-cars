# Multi-stage build for the auth-service module of the connected-cars reactor.
# Build context MUST be the repository root:  docker build -f services/auth-service/Dockerfile .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services ./services
RUN mvn -q -pl services/common-lib,services/auth-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/services/auth-service/target/*.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
