package com.bancoxyz.cuentas.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class ClientesClient {

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

    public ClientesClient(@org.springframework.beans.factory.annotation.Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
                           AuthTokenService authTokenService) {
        this.restClient = builder.baseUrl("lb://clientes-service").build();
        this.authTokenService = authTokenService;
    }

    @TimeLimiter(name = "clientesService")
    @CircuitBreaker(name = "clientesService", fallbackMethod = "fallbackCliente")
    @Retry(name = "clientesService")
    public CompletableFuture<Map<String, Object>> verificarCliente(Long clienteId) {
        return CompletableFuture.supplyAsync(() -> restClient.get()
                .uri("/clientes/{id}", clienteId)
                .headers(h -> h.addAll(authTokenService.authHeader()))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {}));
    }

    public CompletableFuture<Map<String, Object>> fallbackCliente(Long clienteId, Throwable ex) {
        return CompletableFuture.completedFuture(Map.of(
                "clienteId", clienteId,
                "verificado", false,
                "motivo", ex.getClass().getSimpleName()
        ));
    }
}
