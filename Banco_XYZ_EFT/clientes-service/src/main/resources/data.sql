-- Clientes trazables a los titulares presentes en el dataset legacy
-- https://github.com/KariVillagran/fin_legacy_data (intereses_trimestrales.csv)
MERGE INTO clientes (id, nombre, email, telefono, fecha_registro, estado) VALUES
 (1, 'John Doe', 'john.doe@bancoxyz.cl', '+56911111111', '2018-02-10', 'ACTIVO'),
 (2, 'Jane Smith', 'jane.smith@bancoxyz.cl', '+56922222222', '2017-06-23', 'ACTIVO'),
 (3, 'Bob Johnson', 'bob.johnson@bancoxyz.cl', '+56933333333', '2019-11-01', 'ACTIVO'),
 (4, 'Alice Brown', 'alice.brown@bancoxyz.cl', '+56944444444', '2020-04-15', 'ACTIVO'),
 (5, 'Charlie Green', 'charlie.green@bancoxyz.cl', '+56955555555', '2016-09-30', 'ACTIVO'),
 (6, 'Diana Prince', 'diana.prince@bancoxyz.cl', '+56966666666', '2021-01-05', 'ACTIVO'),
 (7, 'Steve Rogers', 'steve.rogers@bancoxyz.cl', '+56977777777', '2015-03-20', 'INACTIVO');
