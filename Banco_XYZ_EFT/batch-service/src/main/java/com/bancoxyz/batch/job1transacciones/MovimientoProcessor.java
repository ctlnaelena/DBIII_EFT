package com.bancoxyz.batch.job1transacciones;

import com.bancoxyz.batch.exception.ConexionTemporalException;
import com.bancoxyz.batch.exception.DatoInvalidoException;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MovimientoProcessor implements ItemProcessor<MovimientoDiarioRaw, MovimientoProcesado> {

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private static final Set<String> TIPOS_VALIDOS = Set.of("debito", "credito");

    private static final ConcurrentHashMap<Long, Boolean> YA_REINTENTADO = new ConcurrentHashMap<>();

    @Override
    public MovimientoProcesado process(MovimientoDiarioRaw raw) {
        Long id = Long.valueOf(raw.getId().trim());

        if (id % 97 == 0 && YA_REINTENTADO.putIfAbsent(id, Boolean.TRUE) == null) {
            throw new ConexionTemporalException("Fallo transitorio simulado de conexion a BD procesando movimiento " + id);
        }

        LocalDate fecha = parsearFecha(raw.getFecha(), id);

        boolean anomalia = false;
        StringBuilder motivo = new StringBuilder();

        Double monto = null;
        String montoTxt = raw.getMonto() == null ? "" : raw.getMonto().trim();
        if (montoTxt.isEmpty()) {
            anomalia = true;
            motivo.append("monto vacio; ");
        } else {
            monto = Double.valueOf(montoTxt);
        }

        String tipo = raw.getTipo() == null ? "" : raw.getTipo().trim().toLowerCase();
        if (!TIPOS_VALIDOS.contains(tipo)) {
            anomalia = true;
            motivo.append("tipo de movimiento invalido ('").append(raw.getTipo()).append("'); ");
        }

        return new MovimientoProcesado(id, fecha, monto, tipo, anomalia,
                anomalia ? motivo.toString().trim() : null);
    }

    private LocalDate parsearFecha(String texto, Long id) {
        if (texto == null || texto.isBlank()) {
            throw new DatoInvalidoException("Movimiento " + id + ": fecha vacia, registro descartado");
        }
        for (DateTimeFormatter formato : FORMATOS) {
            try {
                return LocalDate.parse(texto.trim(), formato);
            } catch (DateTimeParseException ignored) {
                // se intenta con el siguiente formato
            }
        }
        // Fechas como "2024-13-01" (mes 13 invalido) tampoco calzan con
        // ningun formato valido y terminan aqui.
        throw new DatoInvalidoException("Movimiento " + id + ": fecha '" + texto + "' no reconocida, registro descartado");
    }
}
