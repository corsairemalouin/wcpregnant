# Dockerfile multi-stage de WcPregnant
# Contexte de build : racine du monorepo (WcPregnantService + WcPregnantApp)
# Build : docker build -t wcpregnant:local -f WcPregnantService/Dockerfile .
#
# Stage 1 : compilation du backend (Maven)
# Stage 2 : compilation du frontend (Node 22 LTS) -> SPA servie par le service sur /
# Stage 3 : exécution (JRE Alpine) avec l'API + l'interface web

# Stage 1 : build du backend
FROM eclipse-temurin:26-jdk-alpine AS builder
WORKDIR /build
COPY WcPregnantService/pom.xml .
COPY WcPregnantService/src ./src
RUN apk add --no-cache maven && mvn -DskipTests package -q

# Stage 2 : build du frontend
FROM node:22-alpine AS frontend-builder
WORKDIR /app

# Les manifests d'abord : le cache npm n'est invalidé que s'ils changent.
# `--legacy-peer-deps` est indispensable : le lockfile fixe @angular/common 20.0.4
# alors que @angular/platform-browser-dynamic 20.0.7 déclare un peer strict
# @angular/common 20.0.7. npm 10 s'arrête sur ce conflit (ERESOLVE), comme l'install
# local qui a été résolu avec la stratégie "legacy peer".
COPY WcPregnantApp/package.json WcPregnantApp/package-lock.json ./
RUN npm ci --no-audit --no-fund --legacy-peer-deps

COPY WcPregnantApp/ ./
RUN npm run build

# Stage 3 : exécution
FROM eclipse-temurin:26-jre-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

ARG JAR_FILE=/build/target/*.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=prod

WORKDIR /app
COPY --from=builder ${JAR_FILE} app.jar

# Le build Angular est embarqué pour que http://localhost:14007/ affiche
# l'application au lieu de la page de connexion Spring (voir FrontendController).
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