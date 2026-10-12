CREATE TABLE IF NOT EXISTS clientes (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    email VARCHAR(160),
    telefono VARCHAR(40),
    fecha_registro DATE,
    estado VARCHAR(20)
);
