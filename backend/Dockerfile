FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -q -B dependency:go-offline -DskipTests || true
COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/material-platform-1.0.0.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
