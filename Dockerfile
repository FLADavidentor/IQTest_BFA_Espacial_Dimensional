# syntax=docker/dockerfile:1

# ---- Stage 1: build (JDK 21 + Node for the Vite frontend) ----
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src

# Node 20 so the exec-maven-plugin can build the React/Vite SPA into the jar.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates \
 && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && rm -rf /var/lib/apt/lists/*

COPY . .
# Builds frontend (vitest + vite) AND backend jar. Java tests skipped per spec.
RUN chmod +x mvnw && ./mvnw -B package -DskipTests

# ---- Stage 2: run (JRE 21 slim) ----
FROM eclipse-temurin:21-jre-jammy AS run
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*
COPY --from=build /src/target/bfa-espacial-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
