package com.bancoxyz.batch.job3estados;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class TransaccionAnualProcessor implements ItemProcessor<TransaccionAnualRaw, TransaccionAnualProcesada> {

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    @Override
    public TransaccionAnualProcesada process(TransaccionAnualRaw raw) {
        Long cuentaId = Long.valueOf(raw.getCuentaId().trim());

        LocalDate fecha = parsearFecha(raw.getFecha(), cuentaId);

        String montoTxt = raw.getMonto() == null ? "" : raw.getMonto().trim();
        if (montoTxt.isEmpty()) {
            throw new DatoInvalidoException("Cuenta " + cuentaId + ": monto vacio en transaccion del " + raw.getFecha());
        }
        Double monto = Double.valueOf(montoTxt);

        String transaccion = raw.getTransaccion() == null ? "desconocida" : raw.getTransaccion().trim().toLowerCase();
        // Normaliza variantes con tilde del dataset legacy ("depósito" -> "deposito")
        transaccion = transaccion.replace("ó", "o");

        String descripcion = (raw.getDescripcion() == null || raw.getDescripcion().isBlank())
                ? "(sin descripcion)" : raw.getDescripcion().trim();

        return new TransaccionAnualProcesada(cuentaId, fecha, transaccion, monto, descripcion);
    }

    private LocalDate parsearFecha(String texto, Long cuentaId) {
        if (texto == null || texto.isBlank()) {
            throw new DatoInvalidoException("Cuenta " + cuentaId + ": fecha vacia, transaccion descartada");
        }
        for (DateTimeFormatter formato : FORMATOS) {
            try {
                return LocalDate.parse(texto.trim(), formato);
            } catch (DateTimeParseException ignored) {
                // se intenta con el siguiente formato
            }
        }
        throw new DatoInvalidoException("Cuenta " + cuentaId + ": fecha '" + texto + "' no reconocida, transaccion descartada");
    }
}
