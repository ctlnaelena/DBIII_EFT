package com.bancoxyz.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.annotation.OnSkipInRead;
import org.springframework.batch.core.annotation.OnSkipInWrite;

public class SkipLoggerListener<T, S> {

    private static final Logger log = LoggerFactory.getLogger(SkipLoggerListener.class);

    @OnSkipInRead
    public void onSkipInRead(Throwable t) {
        log.warn("[SKIP-LECTURA] Registro omitido durante la lectura: {}", t.getMessage());
    }

    @OnSkipInProcess
    public void onSkipInProcess(T item, Throwable t) {
        log.warn("[SKIP-PROCESO] Registro omitido durante el procesamiento ({}): {}", item, t.getMessage());
    }

    @OnSkipInWrite
    public void onSkipInWrite(S item, Throwable t) {
        log.warn("[SKIP-ESCRITURA] Registro omitido durante la escritura ({}): {}", item, t.getMessage());
    }
}
