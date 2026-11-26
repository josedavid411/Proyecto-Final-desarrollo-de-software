FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/ProyectoFinalDesarolloSoft-1.0-SNAPSHOT.jar app.jar

ENV TASA_DEFECTO=4000.00
ENV PORT=4567

RUN mkdir -p /app/data

EXPOSE 4567
CMD ["java", "-jar", "app.jar"]
