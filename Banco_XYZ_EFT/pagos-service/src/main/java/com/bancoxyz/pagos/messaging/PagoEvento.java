package com.bancoxyz.pagos.messaging;

/**
 * Evento publicado en el topico Kafka "pagos-completados" cada vez que
 * pagos-service procesa (o intenta procesar) una operacion. "fecha" se
 * envia como texto ISO-8601 en vez de LocalDateTime para evitar que
 * Jackson la serialice como arreglo de numeros del lado del productor
 * y el consumidor (otro modulo, con su propio ObjectMapper) no pueda
 * interpretarla.
 */
public record PagoEvento(
        Long cuentaId,
        String tipoOperacion,
        Double monto,
        String estado,
        String fecha
) {
}
