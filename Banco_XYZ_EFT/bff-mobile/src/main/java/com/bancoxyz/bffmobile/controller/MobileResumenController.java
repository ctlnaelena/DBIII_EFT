package com.bancoxyz.bffmobile.controller;

import com.bancoxyz.bffmobile.client.DominioClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mobile")
public class MobileResumenController {

    private final DominioClient dominioClient;

    public MobileResumenController(DominioClient dominioClient) {
        this.dominioClient = dominioClient;
    }

    @GetMapping("/resumen/{clienteId}")
    public Map<String, Object> resumen(@PathVariable Long clienteId) {
        Map<String, Object> cliente = dominioClient.obtenerCliente(clienteId);
        List<Map<String, Object>> cuentas = dominioClient.obtenerCuentasDeCliente(clienteId);

        double saldoTotal = cuentas.stream()
                .mapToDouble(c -> Double.parseDouble(c.get("saldo").toString()))
                .sum();

        List<Map<String, Object>> cuentasResumidas = cuentas.stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.get("id"),
                        "tipo", c.get("tipoCuenta"),
                        "saldo", c.get("saldo")))
                .toList();

        return Map.of(
                "nombre", cliente.get("nombre"),
                "saldoTotal", saldoTotal,
                "cuentas", cuentasResumidas
        );
    }

    @PostMapping("/pagos")
    public Map<String, Object> registrarPago(@RequestBody Map<String, Object> solicitud) {
        return dominioClient.registrarPago(solicitud);
    }
}
