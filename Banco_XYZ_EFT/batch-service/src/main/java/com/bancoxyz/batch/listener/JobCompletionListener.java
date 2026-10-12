package com.bancoxyz.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class JobCompletionListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("=== Iniciando Job [{}] - parametros: {}", jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("=== Job [{}] finalizado con estado {} en {} ms. Pasos: {}",
                jobExecution.getJobInstance().getJobName(), jobExecution.getStatus(),
                jobExecution.getEndTime() != null && jobExecution.getStartTime() != null
                        ? java.time.Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis()
                        : -1,
                jobExecution.getStepExecutions().size());
        jobExecution.getStepExecutions().forEach(step ->
                log.info("    - Step [{}]: leidos={}, escritos={}, omitidos(lectura)={}, omitidos(proceso)={}, omitidos(escritura)={}, reintentos(escritura)={}",
                        step.getStepName(), step.getReadCount(), step.getWriteCount(),
                        step.getReadSkipCount(), step.getProcessSkipCount(), step.getWriteSkipCount(),
                        step.getRollbackCount()));
    }
}
