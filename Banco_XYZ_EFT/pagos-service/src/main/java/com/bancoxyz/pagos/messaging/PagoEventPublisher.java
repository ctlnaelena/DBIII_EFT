package com.bancoxyz.pagos.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PagoEventPublisher {

    private static final String TOPIC_PAGOS = "pagos-completados";
    private static final String TOPIC_ALERTAS = "alertas-seguridad";
    private static final double UMBRAL_ALERTA = 2500.0;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PagoEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicarPago(PagoEvento evento) {
        kafkaTemplate.send(TOPIC_PAGOS, evento.cuentaId().toString(), evento);
        if (evento.monto() != null && evento.monto() >= UMBRAL_ALERTA) {
            kafkaTemplate.send(TOPIC_ALERTAS, evento.cuentaId().toString(), new AlertaSeguridad(
                    evento.cuentaId(), evento.tipoOperacion(), evento.monto(),
                    "Monto superior al umbral de monitoreo (" + UMBRAL_ALERTA + ")", evento.fecha()));
        }
    }
}
