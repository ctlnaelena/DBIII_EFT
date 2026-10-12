package com.bancoxyz.cuentas.messaging;

import com.bancoxyz.cuentas.domain.CuentaBancaria;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PagoEventoListener {

    private static final Logger log = LoggerFactory.getLogger(PagoEventoListener.class);

    private final CuentaRepository repository;

    public PagoEventoListener(CuentaRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "pagos-completados", groupId = "${spring.kafka.consumer.group-id:cuentas-service}")
    public void onPagoCompletado(PagoEventoRecibido evento) {
        if (!"CONFIRMADO".equals(evento.estado())) {
            log.info("Evento de pago ignorado (estado={}) para cuenta {}", evento.estado(), evento.cuentaId());
            return;
        }
        repository.findById(evento.cuentaId()).ifPresentOrElse(cuenta -> {
            double delta = switch (evento.tipoOperacion()) {
                case "DEPOSITO", "TRANSFERENCIA_ENTRADA" -> evento.monto();
                case "RETIRO", "PAGO", "TRANSFERENCIA_SALIDA" -> -evento.monto();
                default -> 0.0;
            };
            cuenta.setSaldo(cuenta.getSaldo() + delta);
            repository.save(cuenta);
            log.info("Saldo actualizado para cuenta {}: nuevo saldo={}", cuenta.getId(), cuenta.getSaldo());
        }, () -> log.warn("Evento de pago recibido para una cuenta inexistente: {}", evento.cuentaId()));
    }
}
