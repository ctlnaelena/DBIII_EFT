# Instrucciones de ejecución y pruebas — Banco XYZ EFT S9

Requisitos: **Java 21**, **Maven**, **Docker Desktop** (abierto). Comandos en PowerShell (Windows);
en macOS/Linux usa `curl` en vez de `curl.exe`. Todo se ejecuta desde la raíz del proyecto (`banco-xyz-eft/`).

## 1. Compilar

```powershell
mvn clean package -DskipTests
```
Debe terminar con `BUILD SUCCESS` para los 10 módulos.

## 2. Levantar todo con Docker Compose

```powershell
docker compose -f docker-compose.yaml -f docker-compose.puertos.yaml up -d --build
docker compose ps
```
La primera vez demora varios minutos. Espera 1–2 minutos hasta que todos los contenedores estén `Up`.
(`docker-compose.puertos.yaml` solo publica los puertos 8081–8083 de los microservicios para poder probarlos directo.)

| Componente | URL |
|---|---|
| Eureka (descubrimiento) | http://localhost:8761 |
| Config Server | http://localhost:8888/cuentas-service/default |
| Auth Server (OAuth2) | http://localhost:9999 |
| BFF Web / Móvil / Cajero | :8091 / :8092 / :8093 |
| Batch | :8080 |

Verifica en Eureka que aparezcan `CLIENTES-SERVICE`, `CUENTAS-SERVICE`, `PAGOS-SERVICE` y los 3 BFF en `UP`.

## 3. Probar seguridad OAuth2.0

```powershell
# Sin token -> 401
curl.exe -s -i http://localhost:8083/clientes

# Token del canal Web (client_credentials)
$TOKEN = (curl.exe -s -X POST http://localhost:9999/oauth2/token -u "bff-web:bffweb-secret" -d "grant_type=client_credentials&scope=clientes.read cuentas.read pagos.read pagos.write" | ConvertFrom-Json).access_token

# Con token -> 200 (lista de 7 clientes)
curl.exe -s http://localhost:8083/clientes -H "Authorization: Bearer $TOKEN"

# Mínimo privilegio: el canal Cajero NO puede leer clientes -> 403
$TOKEN_ATM = (curl.exe -s -X POST http://localhost:9999/oauth2/token -u "bff-atm:bffatm-secret" -d "grant_type=client_credentials&scope=cuentas.read pagos.write" | ConvertFrom-Json).access_token
curl.exe -s -i http://localhost:8083/clientes -H "Authorization: Bearer $TOKEN_ATM"
```

## 4. Probar los 3 BFF

```powershell
curl.exe -s http://localhost:8091/web/dashboard/1      # Web: respuesta completa
curl.exe -s http://localhost:8092/mobile/resumen/1     # Móvil: respuesta liviana
curl.exe -s http://localhost:8093/atm/cuentas/1/saldo  # Cajero: solo saldo
```

## 5. Probar pago + Kafka (actualización asíncrona de saldo)

```powershell
'{"cuentaId":1,"tipoOperacion":"DEPOSITO","monto":500}' | Out-File -Encoding ascii pago.json
curl.exe -s http://localhost:8093/atm/cuentas/1/saldo
curl.exe -s -X POST http://localhost:8091/web/pagos -H "Content-Type: application/json" -d "@pago.json"
Start-Sleep -Seconds 3
curl.exe -s http://localhost:8093/atm/cuentas/1/saldo   # el saldo aumentó en 500

# Evento publicado en Kafka
docker exec bancoxyz-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic pagos-completados --from-beginning --timeout-ms 5000
```
Un pago de monto >= 2500 publica además una alerta en el tópico `alertas-seguridad`.

## 6. Probar Resilience4j (Circuit Breaker, Retry, fallback)

```powershell
docker compose stop cuentas-service
1..6 | ForEach-Object { curl.exe -s -X POST http://localhost:8091/web/pagos -H "Content-Type: application/json" -d "@pago.json"; "" }
curl.exe -s http://localhost:8082/actuator/circuitbreakers
```
Los pagos responden con `estado: FALLBACK` y el circuito `cuentasService` pasa a `OPEN`.
Recuperación: `docker compose start cuentas-service`, espera ~60 s y repite un pago: vuelve a `CONFIRMADO`
y el circuito a `CLOSED`.

## 7. Ejecutar los 3 Jobs de Spring Batch

```powershell
curl.exe -s -X POST http://localhost:8080/batch/transacciones-diarias
curl.exe -s -X POST http://localhost:8080/batch/intereses-mensuales
curl.exe -s -X POST http://localhost:8080/batch/estados-anuales
docker compose logs --tail 40 batch-service
```
Cada respuesta indica, por step, registros leídos, escritos y omitidos (skip). Cada job puede relanzarse
las veces que se quiera (usa un parámetro único por ejecución).

## 8. Escalabilidad horizontal

Usa **solo** `docker-compose.yaml` (los microservicios de dominio no publican puerto fijo):

```powershell
docker compose up -d --scale cuentas-service=3 --scale pagos-service=2 --scale clientes-service=2
docker compose ps
```
En Eureka (http://localhost:8761) se ven varias instancias por servicio. Las llamadas usan
`lb://<servicio>` (Spring Cloud LoadBalancer), por lo que el tráfico se reparte sin reiniciar nada.

## 9. Apagar

```powershell
docker compose down
```

---

## Alternativa: ejecución local sin Docker

Levanta Kafka (`docker run -d --name kafka-local -p 9092:9092 apache/kafka:3.7.0`) y luego, **en este orden**,
cada módulo en su propia terminal con `cd <modulo>` + `mvn spring-boot:run`:

1. `config-server` → 2. `eureka-server` → 3. `auth-server` →
4. `clientes-service`, `cuentas-service`, `pagos-service` →
5. `bff-web`, `bff-mobile`, `bff-atm` → 6. `batch-service`

Las pruebas son las mismas de las secciones 3 a 7 (los puertos 8081–8083 ya quedan accesibles).
