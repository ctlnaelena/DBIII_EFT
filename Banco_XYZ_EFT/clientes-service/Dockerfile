# Etapa 1: compilacion con Maven (usa el pom padre para resolver versiones)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY config-server/pom.xml config-server/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY auth-server/pom.xml auth-server/pom.xml
COPY clientes-service/pom.xml clientes-service/pom.xml
COPY cuentas-service/pom.xml cuentas-service/pom.xml
COPY pagos-service/pom.xml pagos-service/pom.xml
COPY bff-web/pom.xml bff-web/pom.xml
COPY bff-mobile/pom.xml bff-mobile/pom.xml
COPY bff-atm/pom.xml bff-atm/pom.xml
COPY batch-service/pom.xml batch-service/pom.xml
COPY clientes-service/src clientes-service/src
RUN mvn -q -pl clientes-service -am -DskipTests package

# Etapa 2: imagen de ejecucion liviana
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/clientes-service/target/clientes-service.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
