package com.bancoxyz.batch.job2intereses;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

public class TipoCuentaPartitioner implements Partitioner {

    public static final String[] TIPOS = {"ahorro", "prestamo", "hipoteca", "otros"};

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> particiones = new HashMap<>();
        for (String tipo : TIPOS) {
            ExecutionContext context = new ExecutionContext();
            context.putString("tipo", tipo);
            particiones.put("particion-" + tipo, context);
        }
        return particiones;
    }
}
