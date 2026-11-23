FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre
WORKDIR /app


COPY --from=build /app/target/ProyectoFinalDesarolloSoft-1.0-SNAPSHOT.jar app.jar


ENV TASA_DEFECTO=4000.00
ENV PORT=4567


RUN mkdir -p /app/data

EXPOSE 4567
CMD ["java", "-jar", "app.jar"]