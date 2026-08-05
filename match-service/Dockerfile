FROM maven:3.9.16-eclipse-temurin-21-noble AS build
WORKDIR /app
COPY pom.xml .
# --mount=type=cache BILEREK kullanilmiyor: BuildKit cache mount'lari
# `type=gha` katman cache'ine EXPORT EDILMEZ. Onceki halde bu yuzden her CI
# kosusunda tum Maven bagimliliklari sifirdan iniyordu. Normal bir katman
# olarak yazilinca pom.xml degismedigi surece bu adim komple gha cache'inden
# geri yukleniyor.
# -B (batch mode): binlerce satirlik indirme progress ciktisini da susturur.
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests


FROM eclipse-temurin:21.0.11_10-jre-alpine-3.23
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build --chown=appuser:appgroup /app/target/match-app.jar app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

# trigger initial GHCR build
# retrigger to verify GHCR push
