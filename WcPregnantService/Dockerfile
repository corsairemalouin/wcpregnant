# Multi-stage Dockerfile for WcPregnant
# Build context: monorepo root (WcPregnantService + WcPregnantApp)
# Build: docker build -t wcpregnant:local -f WcPregnantService/Dockerfile .
#
# Stage 1: Spring Boot build (Maven)
# Stage 2: Angular build (Node 22 LTS) -> SPA served by the service on /
# Stage 3: Runtime (JRE Alpine) with the API + the web UI

# Stage 1: Build backend
FROM eclipse-temurin:26-jdk-alpine AS builder
WORKDIR /build
COPY WcPregnantService/pom.xml .
COPY WcPregnantService/src ./src
RUN apk add --no-cache maven && mvn -DskipTests package -q

# Stage 2: Build frontend
FROM node:22-alpine AS frontend-builder
WORKDIR /app

# Manifests first: the npm cache is only invalidated when they change.
# `--legacy-peer-deps` is required: the lockfile pins @angular/common 20.0.4 while
# @angular/platform-browser-dynamic 20.0.7 declares a strict peer @angular/common
# 20.0.7. npm 10 aborts (ERESOLVE) on that conflict, exactly like the local install
# which was resolved with the legacy peer strategy.
COPY WcPregnantApp/package.json WcPregnantApp/package-lock.json ./
RUN npm ci --no-audit --no-fund --legacy-peer-deps

COPY WcPregnantApp/ ./
RUN npm run build

# Stage 3: Runtime
FROM eclipse-temurin:26-jre-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

ARG JAR_FILE=/build/target/*.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=prod

WORKDIR /app
COPY --from=builder ${JAR_FILE} app.jar

# The Angular dist is embedded so that http://localhost:14007/ shows the app
# menu instead of the Spring login page (see FrontendController).
COPY --from=frontend-builder /app/dist/WcPregnantApp/browser /app/frontend-dist
ENV FRONTEND_DIST_PATH=/app/frontend-dist

# Port dédié au projet (voir server.port dans application.properties) : il n'est
# pas le port générique 8080, ce qui évite toute collision avec un autre service.
ENV API_PORT=14007
EXPOSE 14007
USER appuser

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:14007/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]