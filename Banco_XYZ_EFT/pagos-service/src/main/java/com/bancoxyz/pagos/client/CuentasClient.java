package com.bancoxyz.pagos.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Cliente hacia cuentas-service (descubierto via Eureka), protegido con
 * Resilience4j: si cuentas-service no responde, el pago cae a fallback
 * y se registra igualmente con estado FALLBACK, sin bloquear al canal.
 */
@Component
public class CuentasClient {

    @Configuration
    static class RestClientConfig {
        @Bean
        @LoadBalanced
        RestClient.Builder loadBalancedRestClientBuilder() {
            return RestClient.builder();
        }
    }

    private final RestClient restClient;
    private final AuthTokenService authTokenService;

    public CuentasClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
                          AuthTokenService authTokenService) {
        this.restClient = builder.baseUrl("lb://cuentas-service").build();
        this.authTokenService = authTokenService;
    }

    @TimeLimiter(name = "cuentasService")
    @CircuitBreaker(name = "cuentasService", fallbackMethod = "fallbackCuenta")
    @Retry(name = "cuentasService")
    public CompletableFuture<Map<String, Object>> obtenerCuenta(Long cuentaId) {
        return CompletableFuture.supplyAsync(() -> restClient.get()
                .uri("/cuentas/{id}", cuentaId)
                .headers(h -> h.addAll(authTokenService.authHeader()))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {}));
    }

    public CompletableFuture<Map<String, Object>> fallbackCuenta(Long cuentaId, Throwable ex) {
        return CompletableFuture.completedFuture(Map.of(
                "cuentaId", cuentaId,
                "fallback", true,
                "motivo", ex.getClass().getSimpleName()
        ));
    }
}
