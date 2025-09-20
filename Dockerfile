FROM openjdk:21-jdk
COPY build/libs/backend-0.0.1-SNAPSHOT.jar /app/myapp.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "myapp.jar"]
