package com.bancoxyz.pagos.repository;

import com.bancoxyz.pagos.domain.PagoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<PagoTransaccion, Long> {
    List<PagoTransaccion> findByCuentaId(Long cuentaId);
}
