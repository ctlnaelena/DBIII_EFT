package com.bancoxyz.batch.config;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.job3estados.TransaccionAnualProcesada;
import com.bancoxyz.batch.job3estados.TransaccionAnualProcessor;
import com.bancoxyz.batch.job3estados.TransaccionAnualRaw;
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
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Job 3: Generacion de Estados de Cuenta Anuales, para auditorias. Un
 * primer step (multi-hilo) normaliza y carga las transacciones del
 * ano; un segundo step (Tasklet) compila el estado de cuenta anual
 * por cuenta (totales de deposito/retiro, saldo neto, cantidad de
 * movimientos), igual que el reporte legacy usado en auditorias.
 */
@Configuration
public class Job3Config {

    @Value("${bancoxyz.batch.data-dir}/estados_financieros_anuales.csv")
    private String archivoEstados;

    @Bean
    public FlatFileItemReader<TransaccionAnualRaw> estadoDelegateReader() {
        return new FlatFileItemReaderBuilder<TransaccionAnualRaw>()
                .name("estadoDelegateReader")
                .resource(new FileSystemResource(archivoEstados))
                .linesToSkip(1)
                .delimited()
                .names("cuentaId", "fecha", "transaccion", "monto", "descripcion")
                .targetType(TransaccionAnualRaw.class)
                .build();
    }

    @Bean
    public ItemStreamReader<TransaccionAnualRaw> estadoReader() {
        SynchronizedItemStreamReader<TransaccionAnualRaw> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(estadoDelegateReader());
        return reader;
    }

    @Bean
    public TransaccionAnualProcessor transaccionAnualProcessor() {
        return new TransaccionAnualProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<TransaccionAnualProcesada> estadoWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<TransaccionAnualProcesada>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO movimientos_estado_cuenta
                        (cuenta_id, fecha, transaccion, monto, descripcion)
                        VALUES (:cuentaId, :fecha, :transaccion, :monto, :descripcion)
                        """)
                .beanMapped()
                .build();
    }

    @Bean
    public TaskExecutor job3TaskExecutor() {
        return new SimpleAsyncTaskExecutor("job3-worker-");
    }

    @Bean
    public Step cargaTransaccionesAnualesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                               ItemStreamReader<TransaccionAnualRaw> estadoReader,
                                               TransaccionAnualProcessor transaccionAnualProcessor,
                                               JdbcBatchItemWriter<TransaccionAnualProcesada> estadoWriter,
                                               TaskExecutor job3TaskExecutor) {
        return new StepBuilder("cargaTransaccionesAnualesStep", jobRepository)
                .<TransaccionAnualRaw, TransaccionAnualProcesada>chunk(25, transactionManager)
                .reader(estadoReader)
                .processor(transaccionAnualProcessor)
                .writer(estadoWriter)
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skipLimit(300)
                .listener(new SkipLoggerListener<TransaccionAnualRaw, TransaccionAnualProcesada>())
                .taskExecutor(job3TaskExecutor)
                .throttleLimit(4)
                .build();
    }

    @Bean
    public org.springframework.batch.core.step.tasklet.Tasklet compilarEstadosAnualesTasklet(JdbcTemplate jdbcTemplate) {
        return (contribution, chunkContext) -> {
            jdbcTemplate.update("DELETE FROM estados_cuenta_anuales");
            jdbcTemplate.update("""
                    INSERT INTO estados_cuenta_anuales
                        (cuenta_id, total_depositos, total_retiros, cantidad_movimientos, saldo_neto, generado_en)
                    SELECT
                        cuenta_id,
                        SUM(CASE WHEN transaccion IN ('deposito') THEN monto ELSE 0 END),
                        SUM(CASE WHEN transaccion IN ('retiro','compra','pago') THEN monto ELSE 0 END),
                        COUNT(*),
                        SUM(monto),
                        CURRENT_TIMESTAMP
                    FROM movimientos_estado_cuenta
                    GROUP BY cuenta_id
                    """);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step compilarEstadosAnualesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                            org.springframework.batch.core.step.tasklet.Tasklet compilarEstadosAnualesTasklet) {
        return new StepBuilder("compilarEstadosAnualesStep", jobRepository)
                .tasklet(compilarEstadosAnualesTasklet, transactionManager)
                .build();
    }

    @Bean
    public Job generacionEstadosCuentaAnualesJob(JobRepository jobRepository,
                                                  Step cargaTransaccionesAnualesStep,
                                                  Step compilarEstadosAnualesStep) {
        return new JobBuilder("generacionEstadosCuentaAnualesJob", jobRepository)
                .listener(new JobCompletionListener())
                .start(cargaTransaccionesAnualesStep)
                .next(compilarEstadosAnualesStep)
                .build();
    }
}
