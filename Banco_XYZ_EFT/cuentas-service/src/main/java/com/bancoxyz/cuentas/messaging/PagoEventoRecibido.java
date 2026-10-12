package com.bancoxyz.cuentas.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PagoEventoRecibido(
        Long cuentaId,
        String tipoOperacion,
        Double monto,
        String estado,
        String fecha
) {
}
