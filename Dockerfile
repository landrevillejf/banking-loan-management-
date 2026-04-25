FROM openjdk:21-jdk-slim

WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew build -x test --no-daemon || true

COPY src ./src
RUN ./gradlew build -x test --no-daemon

EXPOSE 8089

CMD ["java", "-jar", "build/libs/banking-loan-management-0.0.1-SNAPSHOT.jar"]
