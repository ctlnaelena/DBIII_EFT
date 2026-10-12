CREATE TABLE IF NOT EXISTS movimientos_procesados (
    id BIGINT PRIMARY KEY,
    fecha DATE,
    monto DOUBLE,
    tipo VARCHAR(20),
    anomalia BOOLEAN,
    motivo_anomalia VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS resumen_diario (
    total_procesados BIGINT,
    total_anomalias BIGINT,
    generado_en TIMESTAMP
);

CREATE TABLE IF NOT EXISTS intereses_calculados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cuenta_id BIGINT,
    nombre VARCHAR(120),
    saldo DOUBLE,
    tipo VARCHAR(20),
    tasa_aplicada DOUBLE,
    interes_calculado DOUBLE
);

CREATE TABLE IF NOT EXISTS movimientos_estado_cuenta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cuenta_id BIGINT,
    fecha DATE,
    transaccion VARCHAR(30),
    monto DOUBLE,
    descripcion VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS estados_cuenta_anuales (
    cuenta_id BIGINT PRIMARY KEY,
    total_depositos DOUBLE,
    total_retiros DOUBLE,
    cantidad_movimientos BIGINT,
    saldo_neto DOUBLE,
    generado_en TIMESTAMP
);
