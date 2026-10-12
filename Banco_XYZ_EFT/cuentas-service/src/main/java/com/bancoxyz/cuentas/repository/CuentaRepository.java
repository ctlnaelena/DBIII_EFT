package com.bancoxyz.cuentas.repository;

import com.bancoxyz.cuentas.domain.CuentaBancaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuentaRepository extends JpaRepository<CuentaBancaria, Long> {
    List<CuentaBancaria> findByClienteId(Long clienteId);
}
