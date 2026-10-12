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
COPY config-server/src config-server/src
RUN mvn -q -pl config-server -am -DskipTests package

# Etapa 2: imagen de ejecucion liviana
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/config-server/target/config-server.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
