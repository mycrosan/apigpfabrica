FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw -B dependency:go-offline
COPY checkstyle.xml ./
COPY src src
RUN ./mvnw -B package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 gp && useradd --uid 10001 --gid gp --create-home gp
COPY --from=build /build/target/*.war /app/application.war
USER gp
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.war"]
