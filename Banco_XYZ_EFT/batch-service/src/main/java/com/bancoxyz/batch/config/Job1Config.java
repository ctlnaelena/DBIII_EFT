package com.bancoxyz.batch.config;

import com.bancoxyz.batch.exception.ConexionTemporalException;
import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.job1transacciones.MovimientoDiarioRaw;
import com.bancoxyz.batch.job1transacciones.MovimientoProcesado;
import com.bancoxyz.batch.job1transacciones.MovimientoProcessor;
import com.bancoxyz.batch.listener.JobCompletionListener;
import com.bancoxyz.batch.listener.SkipLoggerListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Job 1: Reporte de Transacciones Diarias. Migra el proceso legacy que
 * procesaba todas las transacciones del dia para detectar anomalias y
 * generar un resumen. Step de lectura/procesamiento/escritura
 * multi-hilo (paralelismo) con manejo de errores: registros con fecha
 * no interpretable se omiten (skip) y se cuentan; fallos transitorios
 * de conexion simulados se reintentan automaticamente (retry).
 */
@Configuration
public class Job1Config {

    @Value("${bancoxyz.batch.data-dir}/movimientos_financieros_diarios.csv")
    private String archivoMovimientos;

    @Bean
    public FlatFileItemReader<MovimientoDiarioRaw> movimientoDelegateReader() {
        return new FlatFileItemReaderBuilder<MovimientoDiarioRaw>()
                .name("movimientoDelegateReader")
                .resource(new FileSystemResource(archivoMovimientos))
                .linesToSkip(1)
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .targetType(MovimientoDiarioRaw.class)
                .build();
    }

    @Bean
    public ItemStreamReader<MovimientoDiarioRaw> movimientoReader() {
        SynchronizedItemStreamReader<MovimientoDiarioRaw> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(movimientoDelegateReader());
        return reader;
    }

    @Bean
    public MovimientoProcessor movimientoProcessor() {
        return new MovimientoProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<MovimientoProcesado> movimientoWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<MovimientoProcesado>()
                .dataSource(dataSource)
                .sql("""
                        MERGE INTO movimientos_procesados
                        (id, fecha, monto, tipo, anomalia, motivo_anomalia)
                        KEY(id)
                        VALUES (:id, :fecha, :monto, :tipo, :anomalia, :motivoAnomalia)
                        """)
                .beanMapped()
                .build();
    }

    @Bean
    public TaskExecutor job1TaskExecutor() {
        return new org.springframework.core.task.SimpleAsyncTaskExecutor("job1-worker-");
    }

    @Bean
    public Step movimientosStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                 ItemStreamReader<MovimientoDiarioRaw> movimientoReader,
                                 MovimientoProcessor movimientoProcessor,
                                 JdbcBatchItemWriter<MovimientoProcesado> movimientoWriter,
                                 TaskExecutor job1TaskExecutor) {
        return new StepBuilder("movimientosDiariosStep", jobRepository)
                .<MovimientoDiarioRaw, MovimientoProcesado>chunk(20, transactionManager)
                .reader(movimientoReader)
                .processor(movimientoProcessor)
                .writer(movimientoWriter)
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skipLimit(200)
                .retry(ConexionTemporalException.class)
                .retryLimit(3)
                .listener(new SkipLoggerListener<MovimientoDiarioRaw, MovimientoProcesado>())
                .taskExecutor(job1TaskExecutor)
                .throttleLimit(4)
                .build();
    }

    @Bean
    public org.springframework.batch.core.step.tasklet.Tasklet resumenDiarioTasklet(JdbcTemplate jdbcTemplate) {
        return (contribution, chunkContext) -> {
            jdbcTemplate.update("DELETE FROM resumen_diario");
            jdbcTemplate.update("""
                    INSERT INTO resumen_diario (total_procesados, total_anomalias, generado_en)
                    SELECT COUNT(*), SUM(CASE WHEN anomalia THEN 1 ELSE 0 END), CURRENT_TIMESTAMP
                    FROM movimientos_procesados
                    """);
            return org.springframework.batch.repeat.RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step resumenDiarioStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                   org.springframework.batch.core.step.tasklet.Tasklet resumenDiarioTasklet) {
        return new StepBuilder("resumenDiarioStep", jobRepository)
                .tasklet(resumenDiarioTasklet, transactionManager)
                .build();
    }

    @Bean
    public Job reporteTransaccionesDiariasJob(JobRepository jobRepository, Step movimientosStep, Step resumenDiarioStep) {
        return new JobBuilder("reporteTransaccionesDiariasJob", jobRepository)
                .listener(new JobCompletionListener())
                .start(movimientosStep)
                .next(resumenDiarioStep)
                .build();
    }
}
