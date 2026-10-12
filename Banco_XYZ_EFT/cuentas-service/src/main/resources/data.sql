-- Cuentas iniciales, una por cada cliente seedeado en clientes-service
MERGE INTO cuentas (id, cliente_id, tipo_cuenta, saldo, estado) VALUES
 (1, 1, 'AHORRO', 5000.00, 'ACTIVA'),
 (2, 2, 'PRESTAMO', 8000.00, 'ACTIVA'),
 (3, 3, 'PRESTAMO', 12000.00, 'ACTIVA'),
 (4, 4, 'AHORRO', 0.00, 'ACTIVA'),
 (5, 5, 'HIPOTECA', 7000.00, 'ACTIVA'),
 (6, 6, 'HIPOTECA', 15000.00, 'ACTIVA'),
 (7, 7, 'AHORRO', 10000.00, 'ACTIVA');
ALTER TABLE cuentas ALTER COLUMN id RESTART WITH 8;
