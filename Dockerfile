# Etapa 1: Construcción (compila código usando Maven)
FROM maven:3.9-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (crea una imagen ligera con Java 21)
FROM eclipse-temurin:21-jre-alpine

COPY --from=build /target/*.jar app.jar
# Comando para arrancar aplicación
ENTRYPOINT ["java","-jar","/app.jar"]