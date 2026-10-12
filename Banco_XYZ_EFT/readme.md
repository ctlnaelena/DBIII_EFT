# Banco XYZ — Desarrollo Backend Avanzado: Spring Cloud y Batch


> Repositorio GitHub: `<https://github.com/ctlnaelena/DBIII_EFT.git`

## 1. Resumen del proyecto

Banco XYZ migra su plataforma legacy (COBOL + scripts Shell sobre mainframe) a una
arquitectura moderna de microservicios en la nube. Este proyecto cubre las 3 partes
pedidas por el caso:

1. **Migración de procesos batch legacy a Spring Batch** (`batch-service`).
2. **Patrón Backend for Frontend (BFF)** para 3 canales: Web, Móvil y Cajero
   Automático (`bff-web`, `bff-mobile`, `bff-atm`).
3. **Microservicios resilientes con Spring Cloud**: Gestión de Cuentas, Procesamiento
   de Pagos y Gestión de Clientes, con descubrimiento de servicios (Eureka),
   configuración centralizada (Spring Cloud Config), seguridad distribuida
   (OAuth2.0), tolerancia a fallos (Resilience4j) y mensajería asíncrona (Kafka).

## 2. Los 5 procesos clave identificados para la migración

| # | Proceso | Dónde se implementa |
|---|---|---|
| 1 | Migración de Procesos Batch a Spring Batch | `batch-service` (3 Jobs) |
| 2 | División del sistema monolítico en microservicios | `clientes-service`, `cuentas-service`, `pagos-service` |
| 3 | Implementación del patrón BFF | `bff-web`, `bff-mobile`, `bff-atm` |
| 4 | Seguridad distribuida con Spring Cloud Security (OAuth2.0) | `auth-server` + Resource Servers en cada microservicio |
| 5 | Integración de mensajería asíncrona con Apache Kafka | tópicos `pagos-completados` y `alertas-seguridad` |

## 3. Arquitectura y estructura del repositorio

```
banco-xyz-eft/
├── config-server/        Spring Cloud Config Server (perfil native)
├── eureka-server/         Service Discovery (Netflix Eureka)
├── auth-server/            Authorization Server OAuth2.0 (client_credentials)
├── clientes-service/       Microservicio: Gestión de Clientes
├── cuentas-service/        Microservicio: Gestión de Cuentas (Resilience4j + Kafka)
├── pagos-service/          Microservicio: Procesamiento de Pagos (Resilience4j + Kafka)
├── bff-web/                 BFF canal Web (datos completos)
├── bff-mobile/              BFF canal Móvil (respuestas livianas)
├── bff-atm/                 BFF canal Cajero Automático (saldo + retiro, mínimo privilegio)
├── batch-service/           3 Jobs de Spring Batch (migración de procesos legacy)
├── data/                    CSV legacy de origen (fin_legacy_data)
├── docs/                    Informe técnico, diagramas
├── docker-compose.yaml      Orquestación de todo el ecosistema
├── instrucciones.md         Cómo ejecutar y probar cada componente
└── despliegue.md            Pasos para desplegar en la nube (AWS)
```

### Puertos

| Servicio | Puerto |
|---|---|
| config-server | 8888 |
| eureka-server | 8761 |
| auth-server | 9999 |
| cuentas-service | 8081 |
| pagos-service | 8082 |
| clientes-service | 8083 |
| bff-web | 8091 |
| bff-mobile | 8092 |
| bff-atm | 8093 |
| batch-service | 8080 |
| kafka | 9092 |

### Credenciales OAuth2.0 (client_credentials)

| client_id | client_secret | scopes |
|---|---|---|
| cuentas-service | cuentas-secret | cuentas.read, cuentas.write |
| pagos-service | pagos-secret | pagos.read, pagos.write |
| clientes-service | clientes-secret | clientes.read, clientes.write |
| bff-web | bffweb-secret | todos (canal con mayor privilegio) |
| bff-mobile | bffmobile-secret | cuentas.read, pagos.read/write, clientes.read |
| bff-atm | bffatm-secret | cuentas.read, pagos.write (mínimo privilegio) |

## 4. Origen de los datos

Los 3 archivos usados por `batch-service` provienen del dataset legacy del curso:
<https://github.com/KariVillagran/fin_legacy_data> (`movimientos_financieros_diarios.csv`,
`intereses_trimestrales.csv`, `estados_financieros_anuales.csv`). 

## 5. Instrucciones de ejecución y despliegue

Ver `instrucciones.md` (cómo ejecutar y probar cada componente localmente) y
`despliegue.md` (pasos para desplegar en un entorno de nube).

## 6. Informe técnico

Ver `EFT_S9_Catalina_Portilla.pdf`
