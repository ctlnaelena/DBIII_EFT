package com.bancoxyz.clientes.controller;

import com.bancoxyz.clientes.domain.Cliente;
import com.bancoxyz.clientes.dto.ActualizarEstadoRequest;
import com.bancoxyz.clientes.repository.ClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteRepository repository;

    public ClienteController(ClienteRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Cliente> listar() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtener(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Cliente> actualizarEstado(@PathVariable Long id, @RequestBody ActualizarEstadoRequest request) {
        return repository.findById(id).map(c -> {
            if (request.estado() != null) {
                c.setEstado(request.estado());
            }
            repository.save(c);
            return ResponseEntity.ok(c);
        }).orElse(ResponseEntity.notFound().build());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(IllegalArgumentException.class)
    public void handle() {
    }
}
