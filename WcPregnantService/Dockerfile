# Multi-stage Dockerfile for WcPregnant
# Build: mvn -DskipTests package
# Docker: docker build -t wcpregnant:latest .

# Stage 1: Build
FROM eclipse-temurin:26-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn -DskipTests package -q

# Stage 2: Runtime
FROM eclipse-temurin:26-jre-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

ARG JAR_FILE=/build/target/*.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=prod

WORKDIR /app
COPY --from=builder ${JAR_FILE} app.jar

EXPOSE 8080
USER appuser

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]