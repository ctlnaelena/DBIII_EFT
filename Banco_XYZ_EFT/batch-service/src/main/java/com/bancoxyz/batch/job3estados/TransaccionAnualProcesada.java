package com.bancoxyz.batch.job3estados;

import java.time.LocalDate;

public class TransaccionAnualProcesada {
    private Long cuentaId;
    private LocalDate fecha;
    private String transaccion;
    private Double monto;
    private String descripcion;

    public TransaccionAnualProcesada(Long cuentaId, LocalDate fecha, String transaccion, Double monto, String descripcion) {
        this.cuentaId = cuentaId;
        this.fecha = fecha;
        this.transaccion = transaccion;
        this.monto = monto;
        this.descripcion = descripcion;
    }

    public Long getCuentaId() { return cuentaId; }
    public LocalDate getFecha() { return fecha; }
    public String getTransaccion() { return transaccion; }
    public Double getMonto() { return monto; }
    public String getDescripcion() { return descripcion; }
}
