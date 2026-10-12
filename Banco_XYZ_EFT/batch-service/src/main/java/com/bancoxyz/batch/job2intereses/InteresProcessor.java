package com.bancoxyz.batch.job2intereses;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import org.springframework.batch.item.ItemProcessor;

import java.util.Map;

public class InteresProcessor implements ItemProcessor<CuentaInteresRaw, InteresCalculado> {

    private static final Map<String, Double> TASAS = Map.of(
            "ahorro", 0.005,
            "prestamo", 0.012,
            "hipoteca", 0.008
    );

    private final String tipoParticion;

    public InteresProcessor(String tipoParticion) {
        this.tipoParticion = tipoParticion;
    }

    @Override
    public InteresCalculado process(CuentaInteresRaw raw) {
        String tipoNormalizado = normalizar(raw.getTipo());

        if (!tipoNormalizado.equals(tipoParticion)) {
            return null; // pertenece a otra particion, se descarta aqui
        }

        Long cuentaId = Long.valueOf(raw.getCuentaId().trim());

        if ("otros".equals(tipoParticion)) {
            // tipo de cuenta corrupto/no reconocido (ej: "-1", "unknown"):
            // se contabiliza como anomalia y se descarta (skip).
            throw new DatoInvalidoException("Cuenta " + cuentaId + ": tipo de cuenta invalido ('" + raw.getTipo() + "')");
        }

        String saldoTxt = raw.getSaldo() == null ? "" : raw.getSaldo().trim();
        if (saldoTxt.isEmpty()) {
            throw new DatoInvalidoException("Cuenta " + cuentaId + ": saldo vacio, no se puede calcular interes");
        }
        Double saldo = Double.valueOf(saldoTxt);
        Double tasa = TASAS.get(tipoParticion);
        Double interes = Math.round(saldo * tasa * 100.0) / 100.0;

        String nombre = (raw.getNombre() == null || raw.getNombre().isBlank()) ? "Desconocido" : raw.getNombre().trim();

        return new InteresCalculado(cuentaId, nombre, saldo, tipoParticion, tasa, interes);
    }

    private String normalizar(String tipoCrudo) {
        if (tipoCrudo == null) return "otros";
        String t = tipoCrudo.trim().toLowerCase();
        return switch (t) {
            case "ahorro", "prestamo", "hipoteca" -> t;
            default -> "otros"; // cubre valores corruptos como "-1" o "unknown"
        };
    }
}
