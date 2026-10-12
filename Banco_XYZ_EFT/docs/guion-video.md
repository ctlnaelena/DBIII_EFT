# Guion para el video de presentación (EFT S9)

> Nota: no puedo generar el archivo de video (.mp4) directamente — solo puedo producir
> código, documentos e imágenes. Este guion está pensado para que grabes tú mismo la
> presentación de 5 a 7 minutos con webcam + evidencia en pantalla (Kaltura u otra
> herramienta de grabación), cubriendo exactamente los 4 puntos que exige la pauta
> (criterio 8, 10 puntos por "Completamente Logrado").

## Recomendaciones generales

- Duración objetivo: 5 a 7 minutos (criterio exige respetar el formato/tiempo).
- Webcam encendida durante toda la presentación (o al menos en la introducción y el
  cierre), compartiendo pantalla para mostrar evidencia (código, diagramas, terminal).
- Ten abiertas de antemano: el diagrama de arquitectura (`docs/arquitectura.png`), una
  terminal con el proyecto, y Eureka (`http://localhost:8761`) o los resultados de un
  job de batch ya ejecutado, para no perder tiempo buscando durante la grabación.

---

## 1. Resumen ejecutivo del proyecto (≈ 1 min)

Guion sugerido:

> "Hola, mi nombre es [tu nombre]. Esta es mi Evaluación Final Transversal de
> Desarrollo Backend III, donde modernicé el sistema del Banco XYZ, una institución
> financiera con un sistema legacy en COBOL y scripts Shell sobre mainframe.
> El objetivo fue migrar ese sistema a una arquitectura moderna de microservicios en
> la nube, cubriendo tres frentes: migración de procesos batch a Spring Batch,
> implementación del patrón Backend for Frontend para los canales Web, Móvil y Cajero
> Automático, y el desarrollo de microservicios resilientes y seguros con Spring Cloud."

Muestra en pantalla: `docs/arquitectura.png` mientras hablas, señalando los bloques
(canales → BFFs → microservicios → Kafka/Eureka/Auth → batch-service).

---

## 2. Resultados obtenidos y comparación con el sistema legacy (≈ 2 min)

Puntos a mostrar (evidencia en vivo o capturas):

- Los **5 procesos clave** migrados (nómbralos brevemente): reporte de transacciones
  diarias, cálculo de intereses mensuales, estados de cuenta anuales, gestión de
  clientes/cuentas y procesamiento de pagos.
- Ejecuta en vivo (o muestra una captura ya ejecutada) uno de los jobs de batch, por
  ejemplo:
  ```
  curl -X POST http://localhost:8080/batch/intereses-mensuales
  ```
  y resalta en la respuesta los contadores de leídos/escritos/omitidos (skip) — esto
  evidencia el manejo de datos "sucios" del dataset legacy.
- Muestra Eureka (`http://localhost:8761`) con los microservicios registrados.
- Haz una llamada a uno de los BFF (por ejemplo `GET /mobile/resumen/1`) y compárala
  con la respuesta completa de `GET /web/dashboard/1`, destacando que cada canal
  recibe solo los datos que necesita.

Comparación con el legacy (dilo en voz alta):

> "A diferencia del sistema legacy, donde todo corría en un mainframe monolítico y
> cualquier cambio requería tocar el sistema completo, ahora cada proceso batch y
> cada microservicio se puede desplegar, escalar y mantener de forma independiente,
> y los tres canales dejaron de depender de un único backend que les entregaba a
> todos la misma respuesta."

---

## 3. Desafíos enfrentados y soluciones implementadas (≈ 1.5 min)

Elige 2-3 desafíos reales del proyecto y cuéntalos con tus palabras, por ejemplo:

- **Datos legacy de mala calidad**: los CSV de origen tenían fechas en formatos
  distintos, campos vacíos y tipos de cuenta corruptos. Solución: se implementó
  manejo de excepciones con políticas de *skip* (para registros inválidos) y
  *retry* (para fallos transitorios de conexión), sin detener la ejecución del job.
- **Procesamiento paralelo seguro**: al particionar el cálculo de intereses por tipo
  de cuenta, el reader y el processor debían ser instancias independientes por cada
  partición ejecutándose en paralelo. Solución: se usó `@StepScope` para crear una
  instancia nueva por partición y evitar condiciones de carrera.
- **Resiliencia ante caídas de servicios**: si `cuentas-service` se cae, no se puede
  dejar sin respuesta a `pagos-service`. Solución: Resilience4j con Circuit Breaker,
  Retry y fallback, demostrable deteniendo el microservicio y mostrando cómo el
  circuito pasa a `OPEN` y luego se recupera.

Muestra en pantalla (opcional, muy efectivo): detén `cuentas-service`, dispara un
pago y muestra el fallback en la respuesta, luego vuelve a levantarlo y repite la
llamada mostrando la recuperación automática.

---

## 4. Propuestas de mejora y próximos pasos (≈ 1 min)

Ideas que puedes mencionar (elige las que prefieras o agrega las tuyas):

- Reemplazar las bases H2 locales por Amazon RDS y el contenedor único de Kafka por
  Amazon MSK en el entorno productivo, tal como se documenta en `despliegue.md`.
- Agregar un API Gateway (por ejemplo Spring Cloud Gateway) delante de los 3 BFF
  para centralizar rate-limiting, logging y TLS termination.
- Incorporar trazabilidad distribuida (OpenTelemetry / Zipkin) para seguir una
  transacción completa a través de BFF → microservicio → Kafka → microservicio.
- Automatizar el pipeline de CI/CD (build, test, push a ECR, deploy a ECS) con
  GitHub Actions.
- Agregar pruebas de carga (JMeter/Gatling) para validar el comportamiento de
  Resilience4j y del autoescalado bajo tráfico real.

Cierre sugerido:

> "Con esto concluye mi presentación del proyecto de modernización del Banco XYZ.
> Todo el código fuente, el informe técnico, las instrucciones de ejecución y los
> pasos de despliegue en AWS están disponibles en el repositorio y en la carpeta de
> entrega. Muchas gracias."

---

## Checklist antes de grabar

- [ ] Duración entre 5 y 7 minutos.
- [ ] Webcam visible durante la presentación.
- [ ] Cubriste los 4 puntos: resumen ejecutivo, resultados vs. legacy, desafíos y
      soluciones, propuestas de mejora.
- [ ] Mostraste al menos una evidencia en vivo (diagrama, terminal, Eureka o una
      respuesta de la API).
- [ ] Exportaste el video en formato **MP4** y lo guardaste en la misma carpeta que
      el resto de la entrega (junto a `readme.md`, `informe-tecnico.pdf`,
      `instrucciones.md` y `despliegue.md`).
