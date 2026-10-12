package com.bancoxyz.bffatm.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class DominioClient {

    private final RestClient restClient;
    private final AuthTokenService authTokenService;

    public DominioClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
                          AuthTokenService authTokenService) {
        this.restClient = builder.build();
        this.authTokenService = authTokenService;
    }

    public Map<String, Object> consultarSaldo(Long cuentaId) {
        return restClient.get().uri("lb://cuentas-service/cuentas/{id}", cuentaId)
                .headers(h -> h.addAll(authTokenService.authHeader()))
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, Object> retirar(Map<String, Object> solicitud) {
        return restClient.post().uri("lb://pagos-service/pagos")
                .headers(h -> h.addAll(authTokenService.authHeader()))
                .body(solicitud)
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }
}
