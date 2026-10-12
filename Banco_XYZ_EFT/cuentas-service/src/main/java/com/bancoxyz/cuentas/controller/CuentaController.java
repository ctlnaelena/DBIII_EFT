package com.bancoxyz.cuentas.controller;

import com.bancoxyz.cuentas.client.ClientesClient;
import com.bancoxyz.cuentas.domain.CuentaBancaria;
import com.bancoxyz.cuentas.dto.SolicitudApertura;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cuentas")
public class CuentaController {

    private final CuentaRepository repository;
    private final ClientesClient clientesClient;

    public CuentaController(CuentaRepository repository, ClientesClient clientesClient) {
        this.repository = repository;
        this.clientesClient = clientesClient;
    }

    @GetMapping
    public List<CuentaBancaria> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuentaBancaria> obtener(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cliente/{clienteId}")
    public List<CuentaBancaria> porCliente(@PathVariable Long clienteId) {
        return repository.findByClienteId(clienteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> abrirCuenta(@RequestBody SolicitudApertura solicitud) throws Exception {
        // Verifica al cliente en clientes-service antes de abrir la cuenta;
        // protegido por Resilience4j (Retry + CircuitBreaker + TimeLimiter).
        Map<String, Object> verificacion = clientesClient.verificarCliente(solicitud.clienteId()).get();

        CuentaBancaria cuenta = new CuentaBancaria(
                solicitud.clienteId(), solicitud.tipoCuenta(),
                solicitud.saldoInicial() != null ? solicitud.saldoInicial() : 0.0, "ACTIVA");
        repository.save(cuenta);

        return Map.of("cuenta", cuenta, "verificacionCliente", verificacion);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CuentaBancaria> cerrarCuenta(@PathVariable Long id) {
        return repository.findById(id).map(c -> {
            c.setEstado("CERRADA");
            repository.save(c);
            return ResponseEntity.ok(c);
        }).orElse(ResponseEntity.notFound().build());
    }
}
