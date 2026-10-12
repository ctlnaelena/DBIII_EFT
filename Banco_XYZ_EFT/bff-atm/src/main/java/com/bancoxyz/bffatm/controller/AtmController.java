package com.bancoxyz.bffatm.controller;

import com.bancoxyz.bffatm.client.DominioClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/atm")
public class AtmController {

    private final DominioClient dominioClient;

    public AtmController(DominioClient dominioClient) {
        this.dominioClient = dominioClient;
    }

    @GetMapping("/cuentas/{cuentaId}/saldo")
    public Map<String, Object> consultarSaldo(@PathVariable Long cuentaId) {
        Map<String, Object> cuenta = dominioClient.consultarSaldo(cuentaId);
        return Map.of("cuentaId", cuentaId, "saldo", cuenta.get("saldo"), "estado", cuenta.get("estado"));
    }

    @PostMapping("/cuentas/{cuentaId}/retiro")
    public Map<String, Object> retirar(@PathVariable Long cuentaId, @RequestBody Map<String, Object> body) {
        Map<String, Object> solicitud = Map.of(
                "cuentaId", cuentaId,
                "tipoOperacion", "RETIRO",
                "monto", body.get("monto"));
        return dominioClient.retirar(solicitud);
    }
}
