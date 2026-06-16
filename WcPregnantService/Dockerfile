# Multi-stage Dockerfile simplified: expects the application jar to be built locally (mvn package)
# Build locally: use your Maven (IntelliJ bundled or system mvn) to run `mvn -DskipTests package`.

# Runtime image: use Temurin (adjust java version if necessary)
FROM eclipse-temurin:25-jre as runtime

ARG JAR_FILE=target/*.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"

WORKDIR /app
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

