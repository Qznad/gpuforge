# syntax=docker/dockerfile:1
# Build context = repository root (needs the parent pom).

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY job-service/pom.xml job-service/
COPY compute-service/pom.xml compute-service/
COPY billing-service/pom.xml billing-service/
COPY billing-service/src billing-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl billing-service -am package -DskipTests

# ---------- Stage 2: run ----------
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/billing-service/target/*.jar app.jar
USER app
EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
