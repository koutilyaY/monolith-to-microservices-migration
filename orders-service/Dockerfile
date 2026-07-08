# Build the jar first from the repo root: mvn -DskipTests package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY orders-service/target/orders-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
