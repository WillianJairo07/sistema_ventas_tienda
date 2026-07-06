# Etapa 1: Construcción (compila código usando Maven)
FROM maven:3.9-eclipse-temurin-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (crea una imagen ligera solo con Java para correr app)
FROM eclipse-temurin:17-jre-alpine

COPY --from=build /target/*.jar app.jar
# Comando para arrancar tu aplicación
ENTRYPOINT ["java","-jar","/app.jar"]