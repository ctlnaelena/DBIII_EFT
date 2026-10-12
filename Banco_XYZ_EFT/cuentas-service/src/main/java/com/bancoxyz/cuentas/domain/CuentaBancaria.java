package com.bancoxyz.cuentas.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "cuentas")
public class CuentaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "tipo_cuenta", nullable = false)
    private String tipoCuenta; // AHORRO, CORRIENTE, HIPOTECA, PRESTAMO

    @Column(nullable = false)
    private Double saldo;

    @Column(nullable = false)
    private String estado; // ACTIVA, CERRADA

    protected CuentaBancaria() {
    }

    public CuentaBancaria(Long clienteId, String tipoCuenta, Double saldo, String estado) {
        this.clienteId = clienteId;
        this.tipoCuenta = tipoCuenta;
        this.saldo = saldo;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public String getTipoCuenta() { return tipoCuenta; }
    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
