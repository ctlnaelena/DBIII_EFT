# Guía de evidencias de funcionamiento (para el Informe Técnico)

Objetivo: ir ejecutando cada paso en **PowerShell** (VS Code o Windows Terminal), sacar una captura por cada
bloque y guardarla con el código indicado (`E01.png`, `E02.png`, ...). Después me envías las capturas y yo armo la
sección "Evidencias de funcionamiento" del informe técnico con la descripción de cada una.

## Consejos antes de empezar

- Ejecuta todo desde la raíz del proyecto (`banco-xyz-eft/`). Necesitas Java 21, Maven y Docker Desktop abierto.
- Escribe `Clear-Host` antes de cada comando para que la captura solo muestre lo relevante.
- En cada captura deben verse **el comando y su resultado** juntos. Sube el zoom de la terminal (Ctrl + `+`) para que se lea.
- Para las pruebas con navegador (E03, E04, E29) la captura debe incluir la barra de direcciones.
- Si algún paso falla, **no lo ocultes**: copia el error completo y envíamelo, lo corregimos antes de capturar.
- Usa `curl.exe` (con `.exe`), no `curl` a secas, porque en PowerShell `curl` es un alias distinto.

---

## FASE A — Compilación y arranque de la plataforma

### E01 — Compilación de los 10 módulos
```powershell
Clear-Host
mvn clean package -DskipTests
```
Captura: el final del log con **Reactor Summary** (los 10 módulos en `SUCCESS` y `BUILD SUCCESS`).
Descripción: *Compilación Maven multi-módulo de la solución completa.*

### E02 — Levantar todo con Docker Compose
```powershell
Clear-Host
docker compose -f docker-compose.yaml -f docker-compose.puertos.yaml up -d --build
```
(La primera vez tarda varios minutos.) Espera 1–2 minutos y luego:
```powershell
Clear-Host
docker compose ps
```
Captura: `docker compose ps` con todos los contenedores en estado `Up`.
Descripción: *Orquestación de los 11 contenedores (infraestructura, microservicios, BFF, batch y Kafka).*

> El archivo `docker-compose.puertos.yaml` solo publica los puertos 8081–8083 de los microservicios para poder
> probarlos directamente. Para la demo de escalabilidad (Fase G) se usa solo `docker-compose.yaml`.

### E03 — Eureka (descubrimiento de servicios)
Navegador: `http://localhost:8761`
Captura: sección *Instances currently registered* con CLIENTES-SERVICE, CUENTAS-SERVICE, PAGOS-SERVICE, BFF-WEB, BFF-MOBILE y BFF-ATM en `UP`.
Descripción: *Registro dinámico de los servicios en Eureka Server.*

### E04 — Spring Cloud Config
Navegador: `http://localhost:8888/cuentas-service/default`
Captura: el JSON con las propiedades servidas por el Config Server.
Descripción: *Configuración centralizada entregada por Spring Cloud Config.*

---

## FASE B — Seguridad OAuth2.0

### E05 — Acceso sin token (401)
```powershell
Clear-Host
curl.exe -s -i http://localhost:8083/clientes
```
Captura: línea `HTTP/1.1 401`.
Descripción: *El microservicio de Clientes rechaza peticiones sin token.*

### E06 — Obtención de token (client_credentials)
```powershell
Clear-Host
curl.exe -s -X POST http://localhost:9999/oauth2/token -u "bff-web:bffweb-secret" -d "grant_type=client_credentials&scope=clientes.read cuentas.read pagos.read pagos.write"
```
Captura: JSON con `access_token`, `scope` y `expires_in`.
Descripción: *Auth Server emite un JWT para el canal Web con sus scopes.*

Guarda el token en una variable para los pasos siguientes (no necesita captura):
```powershell
$TOKEN = (curl.exe -s -X POST http://localhost:9999/oauth2/token -u "bff-web:bffweb-secret" -d "grant_type=client_credentials&scope=clientes.read cuentas.read pagos.read pagos.write" | ConvertFrom-Json).access_token
```

### E07 — Acceso con token (200)
```powershell
Clear-Host
curl.exe -s http://localhost:8083/clientes -H "Authorization: Bearer $TOKEN" | ConvertFrom-Json | ConvertTo-Json -Depth 5
```
Captura: la lista de los 7 clientes.
Descripción: *Con token válido el recurso responde correctamente.*

### E08 — Mínimo privilegio del canal ATM (403)
```powershell
Clear-Host
$TOKEN_ATM = (curl.exe -s -X POST http://localhost:9999/oauth2/token -u "bff-atm:bffatm-secret" -d "grant_type=client_credentials&scope=cuentas.read pagos.write" | ConvertFrom-Json).access_token
curl.exe -s -i http://localhost:8083/clientes -H "Authorization: Bearer $TOKEN_ATM"
```
Captura: respuesta `HTTP/1.1 403` (token válido, pero sin scope `clientes.read`).
Descripción: *El canal Cajero no puede acceder a datos de clientes: autorización por scope.*

---

## FASE C — Patrón BFF (3 canales)

### E09 — BFF Web (respuesta completa)
```powershell
Clear-Host
curl.exe -s http://localhost:8091/web/dashboard/1 | ConvertFrom-Json | ConvertTo-Json -Depth 10
```
Descripción: *El BFF Web agrega cliente, cuentas y movimientos en una sola respuesta.*

### E10 — BFF Móvil (respuesta liviana)
```powershell
Clear-Host
curl.exe -s http://localhost:8092/mobile/resumen/1 | ConvertFrom-Json | ConvertTo-Json -Depth 10
```
Descripción: *El BFF Móvil entrega solo nombre, saldo total y cuentas resumidas.*

### E11 — BFF Cajero (operación mínima)
```powershell
Clear-Host
curl.exe -s http://localhost:8093/atm/cuentas/1/saldo
```
Descripción: *El BFF Cajero expone únicamente consulta de saldo (y retiro).*

### E12 — Comparación de tamaño de respuesta Web vs Móvil
```powershell
Clear-Host
"Web:    " + (curl.exe -s http://localhost:8091/web/dashboard/1).Length + " caracteres"
"Movil:  " + (curl.exe -s http://localhost:8092/mobile/resumen/1).Length + " caracteres"
```
Descripción: *La respuesta móvil es considerablemente más liviana (menor consumo de ancho de banda).*

---

## FASE D — Pago, Kafka y actualización asíncrona de saldo

Crea los cuerpos de las peticiones (sin captura):
```powershell
'{"cuentaId":1,"tipoOperacion":"DEPOSITO","monto":500}'  | Out-File -Encoding ascii pago.json
'{"cuentaId":1,"tipoOperacion":"DEPOSITO","monto":3000}' | Out-File -Encoding ascii pago_alto.json
```

### E13 — Saldo antes del pago
```powershell
Clear-Host
curl.exe -s http://localhost:8093/atm/cuentas/1/saldo
```

### E14 — Registrar un pago (BFF Web → pagos-service → cuentas-service)
```powershell
Clear-Host
curl.exe -s -X POST http://localhost:8091/web/pagos -H "Content-Type: application/json" -d "@pago.json"
```
Captura: respuesta con `estado: CONFIRMADO`.
Descripción: *El pago pasa por BFF, valida la cuenta vía Eureka/Resilience4j y se confirma.*

### E15 — Saldo después (actualizado por el consumidor Kafka)
Espera 3 segundos:
```powershell
Clear-Host
Start-Sleep -Seconds 3
curl.exe -s http://localhost:8093/atm/cuentas/1/saldo
```
Captura: el saldo aumentó en 500 respecto a E13 (puedes poner E13 y E15 en la misma captura).
Descripción: *cuentas-service consumió el evento del tópico y actualizó el saldo de forma asíncrona.*

### E16 — Mensajes en el tópico `pagos-completados`
```powershell
Clear-Host
docker exec bancoxyz-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic pagos-completados --from-beginning --timeout-ms 5000
```
Descripción: *Evento publicado por pagos-service en Kafka.*

### E17 — Alerta de seguridad (monto ≥ 2500)
```powershell
Clear-Host
curl.exe -s -X POST http://localhost:8091/web/pagos -H "Content-Type: application/json" -d "@pago_alto.json"
docker exec bancoxyz-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic alertas-seguridad --from-beginning --timeout-ms 5000
```
Descripción: *Los pagos de monto alto generan además un evento en el tópico `alertas-seguridad`.*

---

## FASE E — Resilience4j (Circuit Breaker, Retry y fallback)

### E18 — Simular la caída de cuentas-service
```powershell
Clear-Host
docker compose stop cuentas-service
docker compose ps
```
Descripción: *Se detiene cuentas-service para provocar fallos en pagos-service.*

### E19 — Pagos durante la caída (fallback)
```powershell
Clear-Host
1..6 | ForEach-Object { curl.exe -s -X POST http://localhost:8091/web/pagos -H "Content-Type: application/json" -d "@pago.json"; "" }
```
(Las primeras llamadas demoran por los reintentos.) Captura: respuestas con `estado: FALLBACK`.
Descripción: *Tras agotar reintentos, el servicio responde con el comportamiento alternativo en vez de fallar.*

### E20 — Estado del Circuit Breaker
```powershell
Clear-Host
curl.exe -s http://localhost:8082/actuator/circuitbreakers | ConvertFrom-Json | ConvertTo-Json -Depth 10
```
Captura: `state: OPEN` en el circuito `cuentasService`.
Descripción: *El circuito se abre tras superar el umbral de fallos.*

### E21 — Eventos y logs del circuito
```powershell
Clear-Host
curl.exe -s http://localhost:8082/actuator/circuitbreakerevents
docker compose logs --tail 25 pagos-service
```
Descripción: *Registro de los eventos del Circuit Breaker (errores, `CallNotPermittedException`, cambio de estado).*

### E22 — Recuperación automática
```powershell
Clear-Host
docker compose start cuentas-service
```
Espera ~60 segundos (arranque + registro en Eureka) y repite hasta ver `CONFIRMADO`:
```powershell
Clear-Host
curl.exe -s -X POST http://localhost:8091/web/pagos -H "Content-Type: application/json" -d "@pago.json"
curl.exe -s http://localhost:8082/actuator/circuitbreakers | ConvertFrom-Json | ConvertTo-Json -Depth 10
```
Captura: pago `CONFIRMADO` y circuito en `CLOSED` (o `HALF_OPEN` → `CLOSED`).
Descripción: *El sistema se recupera solo al volver el servicio.*

---

## FASE F — Spring Batch (3 Jobs)

### E23 — Job 1: Reporte de Transacciones Diarias
```powershell
Clear-Host
curl.exe -s -X POST http://localhost:8080/batch/transacciones-diarias | ConvertFrom-Json | ConvertTo-Json -Depth 10
```
Captura: estado `COMPLETED` y, por step, `leidos`, `escritos` y `omitidos` (skip).
Descripción: *Job multi-hilo con manejo de registros inválidos (fechas imposibles, tipos corruptos).*

### E24 — Job 2: Cálculo de Intereses Mensuales (particionado)
```powershell
Clear-Host
curl.exe -s -X POST http://localhost:8080/batch/intereses-mensuales | ConvertFrom-Json | ConvertTo-Json -Depth 10
```
Descripción: *Step particionado en 4 particiones paralelas (ahorro, préstamo, hipoteca, otros).*

### E25 — Job 3: Estados de Cuenta Anuales
```powershell
Clear-Host
curl.exe -s -X POST http://localhost:8080/batch/estados-anuales | ConvertFrom-Json | ConvertTo-Json -Depth 10
```
Descripción: *Carga masiva multi-hilo y compilación de estados anuales por cuenta.*

### E26 — Logs del batch (skips, reintentos y resumen)
```powershell
Clear-Host
docker compose logs --tail 40 batch-service
```
Captura: líneas del `JobCompletionListener` y de omisiones/reintentos.
Descripción: *Trazabilidad de la ejecución: registros omitidos y reintentos ante fallos transitorios.*

### E27 — Reejecución del Job
```powershell
Clear-Host
curl.exe -s -X POST http://localhost:8080/batch/transacciones-diarias | ConvertFrom-Json | ConvertTo-Json -Depth 10
```
Descripción: *El job puede relanzarse (nuevo `jobExecutionId`) sin conflictos con ejecuciones previas.*

---

## FASE G — Escalabilidad horizontal

Este paso usa **solo** `docker-compose.yaml` (sin el override de puertos):

### E28 — Escalar los microservicios
```powershell
Clear-Host
docker compose up -d --scale cuentas-service=3 --scale pagos-service=2 --scale clientes-service=2
docker compose ps
```
Captura: varias instancias (`cuentas-service-1/-2/-3`, `pagos-service-1/-2`, `clientes-service-1/-2`) en `Up`.
Descripción: *Escalado horizontal con un solo comando.*

### E29 — Instancias registradas en Eureka
Espera ~1 minuto. Navegador: `http://localhost:8761`
Captura: CUENTAS-SERVICE con 3 instancias, PAGOS-SERVICE y CLIENTES-SERVICE con 2.
Descripción: *Cada instancia se registra automáticamente y el balanceo de carga las descubre.*

### E30 — El sistema sigue operando con las instancias escaladas
```powershell
Clear-Host
1..5 | ForEach-Object { curl.exe -s http://localhost:8092/mobile/resumen/1; "" }
```
Descripción: *Las peticiones se atienden correctamente con múltiples instancias detrás del balanceador.*

### E31 — Tolerancia a la caída de una instancia
```powershell
Clear-Host
docker stop banco-xyz-eft-cuentas-service-1
1..5 | ForEach-Object { curl.exe -s http://localhost:8092/mobile/resumen/1; "" }
```
(Si el nombre del contenedor es distinto, míralo con `docker compose ps`.)
Descripción: *Al caer una instancia, las otras siguen atendiendo.*

---

## Cierre

```powershell
docker compose down
```

Cuando termines, envíame las capturas (`E01.png` … `E31.png`) y yo agrego al informe técnico la sección
**"Evidencias de funcionamiento"** con cada captura y su descripción. Si alguna falló o no pudiste sacarla,
dime cuál y la omitimos o la reemplazamos.
