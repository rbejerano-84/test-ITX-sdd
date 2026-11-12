FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

RUN chmod +x mvnw && ./mvnw -B clean verify

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/prices-service-*.jar /app/app.jar

USER 10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
