# syntax=docker/dockerfile:1.7

##########################
# 1) Build (dependency + package)
##########################
FROM maven:3.9.16-eclipse-temurin-21-noble AS build
WORKDIR /app

# Önce sadece pom.xml → bağımlılık katmanını ayrı cache'le
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

# Sonra kaynak kod
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q clean package -DskipTests

##########################
# 2) Layered jar extraction
##########################
FROM eclipse-temurin:21.0.11_10-jre-alpine-3.23 AS extract
WORKDIR /app
COPY --from=build /app/target/notification-app.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

##########################
# 3) Runtime
##########################
FROM eclipse-temurin:21.0.11_10-jre-alpine-3.23
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# En az değişenden en çok değişene doğru sırala → layer cache maksimum
COPY --from=extract --chown=appuser:appgroup /app/dependencies/ ./
COPY --from=extract --chown=appuser:appgroup /app/spring-boot-loader/ ./
COPY --from=extract --chown=appuser:appgroup /app/snapshot-dependencies/ ./
COPY --from=extract --chown=appuser:appgroup /app/application/ ./

USER appuser
EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", \
            "org.springframework.boot.loader.launch.JarLauncher"]
