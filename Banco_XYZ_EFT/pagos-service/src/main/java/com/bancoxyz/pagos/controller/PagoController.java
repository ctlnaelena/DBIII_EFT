package com.bancoxyz.pagos.controller;

import com.bancoxyz.pagos.client.CuentasClient;
import com.bancoxyz.pagos.domain.PagoTransaccion;
import com.bancoxyz.pagos.dto.SolicitudPago;
import com.bancoxyz.pagos.messaging.PagoEvento;
import com.bancoxyz.pagos.messaging.PagoEventPublisher;
import com.bancoxyz.pagos.repository.PagoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pagos")
public class PagoController {

    private final CuentasClient cuentasClient;
    private final PagoEventPublisher eventPublisher;
    private final PagoRepository repository;

    public PagoController(CuentasClient cuentasClient, PagoEventPublisher eventPublisher, PagoRepository repository) {
        this.cuentasClient = cuentasClient;
        this.eventPublisher = eventPublisher;
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> procesarPago(@RequestBody SolicitudPago solicitud) throws Exception {
        Map<String, Object> cuenta = cuentasClient.obtenerCuenta(solicitud.cuentaId()).get();
        boolean fueFallback = Boolean.TRUE.equals(cuenta.get("fallback"));
        String estado = fueFallback ? "FALLBACK" : "CONFIRMADO";

        PagoTransaccion registro = new PagoTransaccion(
                solicitud.cuentaId(), solicitud.tipoOperacion(), solicitud.monto(), estado, LocalDateTime.now());
        repository.save(registro);

        eventPublisher.publicarPago(new PagoEvento(
                solicitud.cuentaId(), solicitud.tipoOperacion(), solicitud.monto(), estado,
                LocalDateTime.now().toString()));

        return Map.of("pago", registro, "cuentaVerificada", cuenta);
    }

    @GetMapping
    public List<PagoTransaccion> historial() {
        return repository.findAll();
    }

    @GetMapping("/cuenta/{cuentaId}")
    public List<PagoTransaccion> porCuenta(@PathVariable Long cuentaId) {
        return repository.findByCuentaId(cuentaId);
    }
}
