FROM amazoncorretto:21-alpine3.21-jdk

WORKDIR /app

COPY . /app

ENV DB_URL=jdbc:postgresql://localhost:5432/postgres
ENV DB_USERNAME=postgres
ENV DB_PASSWORD=password

RUN ./mvnw clean package -DskipTests
EXPOSE 8080
CMD ["java", "-jar", "target/server-0.0.1-SNAPSHOT.jar"]
