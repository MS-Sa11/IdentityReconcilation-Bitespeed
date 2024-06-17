FROM eclipse-temurin:22-jdk-alpine
VOLUME /tmp
COPY target/identityreconcilation-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
