FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the compiled JAR artifact from Gradle build output
COPY build/libs/api-gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]