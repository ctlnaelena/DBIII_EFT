package com.bancoxyz.batch.config;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.job2intereses.CuentaInteresRaw;
import com.bancoxyz.batch.job2intereses.InteresCalculado;
import com.bancoxyz.batch.job2intereses.InteresProcessor;
import com.bancoxyz.batch.job2intereses.TipoCuentaPartitioner;
import com.bancoxyz.batch.listener.JobCompletionListener;
import com.bancoxyz.batch.listener.SkipLoggerListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.SimpleStepExecutionSplitter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Job 2: Calculo de Intereses Mensuales, particionado por tipo de
 * cuenta (ahorro / prestamo / hipoteca / otros). Cada particion
 * procesa en paralelo su propio subconjunto de filas aplicando la
 * tasa correspondiente, demostrando escalabilidad horizontal del
 * procesamiento batch.
 */
@Configuration
public class Job2Config {

    @Value("${bancoxyz.batch.data-dir}/intereses_trimestrales.csv")
    private String archivoIntereses;

    @Bean
    @StepScope
    public FlatFileItemReader<CuentaInteresRaw> interesReader() {
        // Cada particion (bean @StepScope) obtiene su propia instancia de
        // reader con su propio cursor de lectura, ya que las particiones
        // se ejecutan en paralelo en hilos distintos. El filtrado por tipo
        // ocurre en InteresProcessor, que conoce el tipo de su particion
        // via el ExecutionContext del step.
        return new FlatFileItemReaderBuilder<CuentaInteresRaw>()
                .name("interesReader")
                .resource(new FileSystemResource(archivoIntereses))
                .linesToSkip(1)
                .delimited()
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .targetType(CuentaInteresRaw.class)
                .build();
    }

    @Bean
    @StepScope
    public InteresProcessor interesProcessor(@Value("#{stepExecutionContext['tipo']}") String tipo) {
        return new InteresProcessor(tipo);
    }

    @Bean
    public JdbcBatchItemWriter<InteresCalculado> interesWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<InteresCalculado>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO intereses_calculados
                        (cuenta_id, nombre, saldo, tipo, tasa_aplicada, interes_calculado)
                        VALUES (:cuentaId, :nombre, :saldo, :tipo, :tasaAplicada, :interesCalculado)
                        """)
                .beanMapped()
                .build();
    }

    @Bean
    public TaskExecutor job2TaskExecutor() {
        return new SimpleAsyncTaskExecutor("job2-partition-");
    }

    @Bean
    public Step interesWorkerStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                                   FlatFileItemReader<CuentaInteresRaw> interesReader,
                                   InteresProcessor interesProcessor,
                                   JdbcBatchItemWriter<InteresCalculado> interesWriter) {
        return new StepBuilder("interesWorkerStep", jobRepository)
                .<CuentaInteresRaw, InteresCalculado>chunk(20, transactionManager)
                .reader(interesReader)
                .processor(interesProcessor)
                .writer(interesWriter)
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skipLimit(600)
                .listener(new SkipLoggerListener<CuentaInteresRaw, InteresCalculado>())
                .build();
    }

    @Bean
    public Step calculoInteresesPartitionedStep(JobRepository jobRepository,
                                                 Step interesWorkerStep,
                                                 TaskExecutor job2TaskExecutor) {
        return new StepBuilder("calculoInteresesPartitionedStep", jobRepository)
                .partitioner("interesWorkerStep", new TipoCuentaPartitioner())
                .step(interesWorkerStep)
                .taskExecutor(job2TaskExecutor)
                .gridSize(TipoCuentaPartitioner.TIPOS.length)
                .build();
    }

    @Bean
    public Job calculoInteresesMensualesJob(JobRepository jobRepository, Step calculoInteresesPartitionedStep) {
        return new JobBuilder("calculoInteresesMensualesJob", jobRepository)
                .listener(new JobCompletionListener())
                .start(calculoInteresesPartitionedStep)
                .build();
    }
}
