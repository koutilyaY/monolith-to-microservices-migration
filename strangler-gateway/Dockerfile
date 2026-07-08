# Build the jar first from the repo root: mvn -DskipTests package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY strangler-gateway/target/strangler-gateway.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
