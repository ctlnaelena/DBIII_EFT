package com.bancoxyz.batch.job1transacciones;

import java.time.LocalDate;

public class MovimientoProcesado {
    private Long id;
    private LocalDate fecha;
    private Double monto;
    private String tipo;
    private boolean anomalia;
    private String motivoAnomalia;

    public MovimientoProcesado(Long id, LocalDate fecha, Double monto, String tipo, boolean anomalia, String motivoAnomalia) {
        this.id = id;
        this.fecha = fecha;
        this.monto = monto;
        this.tipo = tipo;
        this.anomalia = anomalia;
        this.motivoAnomalia = motivoAnomalia;
    }

    public Long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public Double getMonto() { return monto; }
    public String getTipo() { return tipo; }
    public boolean isAnomalia() { return anomalia; }
    public String getMotivoAnomalia() { return motivoAnomalia; }
}
