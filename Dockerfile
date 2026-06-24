# syntax=docker/dockerfile:1

# ---- Build stage: compile and package the runnable jar from repo contents ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Resolve dependencies first so they cache across source-only changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build the shaded jar (tests are run separately via `mvn test`).
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Run stage: small JRE image that just runs the game --------------------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/uno.jar ./uno.jar

# Default to a short bot game; override args at `docker run` time.
ENTRYPOINT ["java", "-jar", "uno.jar"]
CMD ["--bots", "3", "--games", "1", "--quiet"]
