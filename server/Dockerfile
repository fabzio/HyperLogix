FROM eclipse-temurin:21-jre-alpine

ENV DB_HOST=localhost:3306
ENV DB_USERNAME=admin
ENV DB_PASSWORD=admin

WORKDIR /app

COPY target/server-*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
