# Despliegue en la nube (AWS) — Banco XYZ EFT S9

Este documento describe el proceso para desplegar el ecosistema de Banco XYZ en AWS,
reutilizando las mismas imágenes Docker ya construidas y validadas localmente con
`docker-compose.yaml`. Se eligió **Amazon ECS (Elastic Container Service) con Fargate**
como objetivo de despliegue porque:

- No requiere administrar servidores (EC2) ni un control plane de Kubernetes, lo que
  reduce drásticamente el costo operativo para un banco que recién está migrando
  desde mainframe — justo el tipo de organización descrito en el caso.
- Escala horizontalmente cada microservicio de forma independiente (Service Auto
  Scaling), igual que se demostró localmente con `docker compose up --scale`.
- Se integra nativamente con ECR (registro de imágenes), Secrets Manager (credenciales
  OAuth2.0) y CloudWatch (logs y métricas), sin herramientas adicionales.

## 1. Arquitectura de despliegue

```
Internet
   │
   ▼
Application Load Balancer (ALB)
   │        │        │
   ▼        ▼        ▼
bff-web  bff-mobile  bff-atm        (ECS Services, Fargate, auto-scaling)
   │        │        │
   └────────┴────────┘
            │
            ▼
  Cloud Map (Service Discovery de AWS, o Eureka dentro del VPC)
            │
   ┌────────┼────────┐
   ▼        ▼        ▼
clientes  cuentas   pagos-service    (ECS Services, Fargate, auto-scaling,
service   service                     sin IP/puerto publico)
   │        │        │
   └────────┴────────┘
            │
            ▼
   Amazon MSK (Managed Streaming for Kafka)
            │
   auth-server, eureka-server, config-server   (ECS Services internos)
            │
   Amazon RDS / Aurora (reemplaza las bases H2 locales en producción)
```

## 2. Prerrequisitos

- Cuenta de AWS con permisos para ECS, ECR, VPC, IAM, Secrets Manager y MSK.
- AWS CLI configurado (`aws configure`) con una región, por ejemplo `us-east-1`.
- Docker instalado localmente (ya usado para `docker compose build`).

## 3. Paso a paso

### 3.1 Crear un repositorio ECR por microservicio

```bash
for servicio in config-server eureka-server auth-server clientes-service \
                cuentas-service pagos-service bff-web bff-mobile bff-atm batch-service; do
  aws ecr create-repository --repository-name banco-xyz/$servicio
done
```

### 3.2 Autenticar Docker contra ECR y publicar las imágenes

```bash
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

docker compose build

for servicio in config-server eureka-server auth-server clientes-service \
                cuentas-service pagos-service bff-web bff-mobile bff-atm batch-service; do
  docker tag banco-xyz-eft-$servicio:latest \
    <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/banco-xyz/$servicio:latest
  docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/banco-xyz/$servicio:latest
done
```

### 3.3 Red (VPC) y Service Discovery

1. Crear una VPC con al menos 2 subredes privadas (para los microservicios internos:
   `clientes-service`, `cuentas-service`, `pagos-service`, `auth-server`,
   `eureka-server`, `config-server`, `batch-service`) y 2 subredes públicas (para el
   ALB que expone los 3 BFF).
2. Crear un namespace de **AWS Cloud Map** (`bancoxyz.local`) para que los servicios
   internos se resuelvan por DNS dentro de la VPC — complementa a Eureka, que sigue
   siendo la fuente de verdad para el balanceo de carga a nivel de aplicación
   (Spring Cloud LoadBalancer).

### 3.4 Secrets Manager

Guardar los client secrets OAuth2.0 y las credenciales de base de datos como
secretos, en vez de dejarlos en `application.yml`:

```bash
aws secretsmanager create-secret --name banco-xyz/cuentas-service/client-secret \
  --secret-string "cuentas-secret"
# repetir para pagos-service, clientes-service, bff-web, bff-mobile, bff-atm
```

Las Task Definitions de ECS referencian estos secretos como variables de entorno
(`valueFrom`), sobrescribiendo `bancoxyz.auth.client-secret`.

### 3.5 Base de datos gestionada

H2 (usado en el entorno académico) se reemplaza por **Amazon RDS for PostgreSQL**
(una instancia por microservicio, o esquemas separados en una instancia compartida
para reducir costos en el ambiente de pruebas). Solo cambia `spring.datasource.url`
y el driver en cada Task Definition; el código de los repositorios JPA no cambia.

### 3.6 Mensajería: Amazon MSK

Se reemplaza el contenedor único de Kafka por un clúster **Amazon MSK** (mínimo 2
brokers en modo multi-AZ para alta disponibilidad). Solo cambia
`spring.kafka.bootstrap-servers` apuntando a los endpoints de MSK; el código de
productores/consumidores no cambia.

### 3.7 Definir las Task Definitions y Services de ECS

Para cada microservicio (ejemplo con `cuentas-service`):

```bash
aws ecs register-task-definition --cli-input-json file://ecs/cuentas-service-task.json

aws ecs create-service \
  --cluster banco-xyz-cluster \
  --service-name cuentas-service \
  --task-definition cuentas-service \
  --desired-count 3 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-priv-1,subnet-priv-2],securityGroups=[sg-internos]}" \
  --service-registries "registryArn=<arn-cloud-map-cuentas-service>"
```

`--desired-count 3` ya despliega 3 instancias del microservicio desde el inicio
(equivalente a `docker compose up --scale cuentas-service=3` en local), cumpliendo
el requerimiento de escalabilidad horizontal.

### 3.8 Auto Scaling

```bash
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/banco-xyz-cluster/cuentas-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 --max-capacity 8

aws application-autoscaling put-scaling-policy \
  --policy-name cuentas-service-cpu-scaling \
  --service-namespace ecs \
  --resource-id service/banco-xyz-cluster/cuentas-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{"TargetValue":60.0,"PredefinedMetricSpecification":{"PredefinedMetricType":"ECSServiceAverageCPUUtilization"}}'
```

### 3.9 Exponer los BFF al exterior

Crear un **Application Load Balancer** público con 3 listeners/target groups (uno
por BFF), apuntando a los Services `bff-web`, `bff-mobile` y `bff-atm` en las
subredes públicas, con certificado TLS (AWS Certificate Manager) para HTTPS.

### 3.10 El Job de Spring Batch en la nube

`batch-service` se despliega como una **ECS Scheduled Task** (usando EventBridge
Scheduler) en vez de un servicio permanente, ya que los 3 procesos batch (reporte
diario, cálculo de intereses mensual, estados de cuenta anual) corren en una
ventana de tiempo y no necesitan estar siempre activos — esto reduce el costo frente
a tenerlo como servicio 24/7.

## 4. Monitoreo

- **CloudWatch Logs**: cada contenedor envía su salida estándar automáticamente.
- **CloudWatch Container Insights**: métricas de CPU/memoria por servicio.
- Los endpoints de Actuator (`/actuator/health`, `/actuator/circuitbreakerevents`)
  quedan disponibles internamente para health checks de ECS y para un dashboard de
  observabilidad (por ejemplo, Grafana apuntando a CloudWatch o Prometheus).

## 5. Resumen de equivalencias local → nube

| Local (docker-compose) | AWS |
|---|---|
| Contenedores Docker | Tareas Fargate en ECS |
| `docker compose up --scale` | `desired-count` + Application Auto Scaling |
| H2 embebido | Amazon RDS (PostgreSQL) |
| Contenedor único de Kafka | Amazon MSK |
| Red `bancoxyz-net` | VPC privada + Cloud Map |
| Puertos publicados en el host | Application Load Balancer + Target Groups |
| Variables de entorno en `docker-compose.yaml` | AWS Secrets Manager + Task Definitions |
