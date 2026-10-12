package com.bancoxyz.pagos.messaging;

/**
 * Evento publicado en el topico Kafka "alertas-seguridad" cuando una
 * operacion supera el umbral de monto configurado, para que un
 * consumidor de monitoreo/fraude pueda reaccionar en tiempo real.
 */
public record AlertaSeguridad(
        Long cuentaId,
        String tipoOperacion,
        Double monto,
        String motivo,
        String fecha
) {
}
