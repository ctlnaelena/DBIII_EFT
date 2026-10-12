package com.bancoxyz.bffweb.controller;

import com.bancoxyz.bffweb.client.DominioClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/web")
public class WebDashboardController {

    private final DominioClient dominioClient;

    public WebDashboardController(DominioClient dominioClient) {
        this.dominioClient = dominioClient;
    }

    @GetMapping("/dashboard/{clienteId}")
    public Map<String, Object> dashboard(@PathVariable Long clienteId) {
        Map<String, Object> cliente = dominioClient.obtenerCliente(clienteId);
        List<Map<String, Object>> cuentas = dominioClient.obtenerCuentasDeCliente(clienteId);

        List<Map<String, Object>> cuentasConMovimientos = cuentas.stream().map(cuenta -> {
            Object idCuenta = cuenta.get("id");
            List<Map<String, Object>> movimientos = dominioClient.obtenerPagosDeCuenta(
                    Long.valueOf(idCuenta.toString()));
            return Map.of("cuenta", cuenta, "movimientos", movimientos);
        }).toList();

        return Map.of("cliente", cliente, "cuentas", cuentasConMovimientos);
    }

    @PostMapping("/pagos")
    public Map<String, Object> registrarPago(@RequestBody Map<String, Object> solicitud) {
        return dominioClient.registrarPago(solicitud);
    }
}
