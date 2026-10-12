package com.bancoxyz.pagos.dto;

public record SolicitudPago(Long cuentaId, String tipoOperacion, Double monto) {
}
