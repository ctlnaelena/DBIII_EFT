package com.bancoxyz.batch.exception;

public class ConexionTemporalException extends RuntimeException {
    public ConexionTemporalException(String mensaje) {
        super(mensaje);
    }

    public ConexionTemporalException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
