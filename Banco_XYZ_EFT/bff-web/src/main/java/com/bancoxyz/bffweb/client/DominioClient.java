package com.bancoxyz.bffweb.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class DominioClient {

    private final RestClient clientes;
    private final RestClient cuentas;
    private final RestClient pagos;
    private final AuthTokenService authTokenService;

    public DominioClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
                          AuthTokenService authTokenService) {
        this.clientes = builder.build();
        this.cuentas = builder.build();
        this.pagos = builder.build();
        this.authTokenService = authTokenService;
    }

    public Map<String, Object> obtenerCliente(Long clienteId) {
        return clientes.get().uri("lb://clientes-service/clientes/{id}", clienteId)
                .headers(h -> h.addAll(authTokenService.authHeader()))
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }

    public List<Map<String, Object>> obtenerCuentasDeCliente(Long clienteId) {
        return cuentas.get().uri("lb://cuentas-service/cuentas/cliente/{id}", clienteId)
                .headers(h -> h.addAll(authTokenService.authHeader()))
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }

    public List<Map<String, Object>> obtenerPagosDeCuenta(Long cuentaId) {
        return pagos.get().uri("lb://pagos-service/pagos/cuenta/{id}", cuentaId)
                .headers(h -> h.addAll(authTokenService.authHeader()))
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, Object> registrarPago(Map<String, Object> solicitud) {
        return pagos.post().uri("lb://pagos-service/pagos")
                .headers(h -> h.addAll(authTokenService.authHeader()))
                .body(solicitud)
                .retrieve().body(new ParameterizedTypeReference<>() {});
    }
}
