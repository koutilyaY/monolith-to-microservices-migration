# Build the jar first from the repo root: mvn -DskipTests package
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY legacy-monolith/target/legacy-monolith.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
