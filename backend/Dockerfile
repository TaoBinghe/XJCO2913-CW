FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ENV PORT=80
ENV JAVA_OPTS=""

COPY target/backend-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 80

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar /app/app.jar"]