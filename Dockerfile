FROM openjdk:21-jdk-slim
COPY build/libs/backend-0.0.1-SNAPSHOT.jar /app.jar

ENV SPRING_PROFILES_ACTIVE=server

ENTRYPOINT ["java","-jar","/app.jar"]