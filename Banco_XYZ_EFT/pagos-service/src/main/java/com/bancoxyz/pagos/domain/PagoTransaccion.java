package com.bancoxyz.pagos.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Microservicio de Procesamiento de Pagos: procesa pagos, transferencias
 * y depositos. El saldo real de la cuenta vive en cuentas-service y se
 * actualiza de forma asincrona via el evento Kafka "pago.completado".
 */
@Entity
@Table(name = "pagos")
public class PagoTransaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(name = "tipo_operacion", nullable = false)
    private String tipoOperacion; // DEPOSITO, RETIRO, PAGO, TRANSFERENCIA_SALIDA, TRANSFERENCIA_ENTRADA

    @Column(nullable = false)
    private Double monto;

    @Column(nullable = false)
    private String estado; // CONFIRMADO, FALLBACK

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected PagoTransaccion() {
    }

    public PagoTransaccion(Long cuentaId, String tipoOperacion, Double monto, String estado, LocalDateTime fecha) {
        this.cuentaId = cuentaId;
        this.tipoOperacion = tipoOperacion;
        this.monto = monto;
        this.estado = estado;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Long getCuentaId() { return cuentaId; }
    public String getTipoOperacion() { return tipoOperacion; }
    public Double getMonto() { return monto; }
    public String getEstado() { return estado; }
    public LocalDateTime getFecha() { return fecha; }
}
