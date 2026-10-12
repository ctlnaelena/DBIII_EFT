package com.bancoxyz.batch.job2intereses;

public class InteresCalculado {
    private Long cuentaId;
    private String nombre;
    private Double saldo;
    private String tipo;
    private Double tasaAplicada;
    private Double interesCalculado;

    public InteresCalculado(Long cuentaId, String nombre, Double saldo, String tipo, Double tasaAplicada, Double interesCalculado) {
        this.cuentaId = cuentaId;
        this.nombre = nombre;
        this.saldo = saldo;
        this.tipo = tipo;
        this.tasaAplicada = tasaAplicada;
        this.interesCalculado = interesCalculado;
    }

    public Long getCuentaId() { return cuentaId; }
    public String getNombre() { return nombre; }
    public Double getSaldo() { return saldo; }
    public String getTipo() { return tipo; }
    public Double getTasaAplicada() { return tasaAplicada; }
    public Double getInteresCalculado() { return interesCalculado; }
}
