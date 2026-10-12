package com.bancoxyz.batch.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/batch")
public class BatchJobController {

    private final JobLauncher jobLauncher;
    private final Job reporteTransaccionesDiariasJob;
    private final Job calculoInteresesMensualesJob;
    private final Job generacionEstadosCuentaAnualesJob;

    public BatchJobController(JobLauncher jobLauncher,
                               Job reporteTransaccionesDiariasJob,
                               Job calculoInteresesMensualesJob,
                               Job generacionEstadosCuentaAnualesJob) {
        this.jobLauncher = jobLauncher;
        this.reporteTransaccionesDiariasJob = reporteTransaccionesDiariasJob;
        this.calculoInteresesMensualesJob = calculoInteresesMensualesJob;
        this.generacionEstadosCuentaAnualesJob = generacionEstadosCuentaAnualesJob;
    }

    @PostMapping("/transacciones-diarias")
    public Map<String, Object> lanzarTransaccionesDiarias() throws Exception {
        return lanzar(reporteTransaccionesDiariasJob);
    }

    @PostMapping("/intereses-mensuales")
    public Map<String, Object> lanzarInteresesMensuales() throws Exception {
        return lanzar(calculoInteresesMensualesJob);
    }

    @PostMapping("/estados-anuales")
    public Map<String, Object> lanzarEstadosAnuales() throws Exception {
        return lanzar(generacionEstadosCuentaAnualesJob);
    }

    private Map<String, Object> lanzar(Job job) throws Exception {
        JobExecution ejecucion = jobLauncher.run(job, new JobParametersBuilder()
                .addString("ejecutadoEn", LocalDateTime.now().toString())
                .toJobParameters());
        return Map.of(
                "job", job.getName(),
                "jobExecutionId", ejecucion.getId(),
                "estado", ejecucion.getStatus().toString(),
                "pasos", ejecucion.getStepExecutions().stream()
                        .map(s -> Map.of(
                                "step", s.getStepName(),
                                "leidos", s.getReadCount(),
                                "escritos", s.getWriteCount(),
                                "omitidosLectura", s.getReadSkipCount(),
                                "omitidosProceso", s.getProcessSkipCount(),
                                "omitidosEscritura", s.getWriteSkipCount()))
                        .toList()
        );
    }
}
