# Etapa 1: Construcción
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copiamos solo el jar generado desde la etapa de construcción
COPY --from=build /app/target/*.jar app.jar

# Comando para arrancar
ENTRYPOINT ["java", "-jar", "app.jar"]