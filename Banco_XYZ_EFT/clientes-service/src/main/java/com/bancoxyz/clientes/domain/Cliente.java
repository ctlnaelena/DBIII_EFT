package com.bancoxyz.clientes.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String email;

    private String telefono;

    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro;

    @Column(name = "estado")
    private String estado; // ACTIVO, INACTIVO

    protected Cliente() {
    }

    public Cliente(Long id, String nombre, String email, String telefono, LocalDate fechaRegistro, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.fechaRegistro = fechaRegistro;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
