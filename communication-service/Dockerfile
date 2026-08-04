FROM maven:3.9.16-eclipse-temurin-21-noble AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests


FROM eclipse-temurin:21.0.11_10-jre-alpine-3.23
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build --chown=appuser:appgroup /app/target/communication-app.jar app.jar
USER appuser
EXPOSE 8084
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

# trigger initial GHCR build
# retrigger to verify GHCR push
