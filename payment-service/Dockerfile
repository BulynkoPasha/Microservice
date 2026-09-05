FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]