package com.bancoxyz.cuentas.client;

import com.bancoxyz.cuentas.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;

/**
 * Obtiene y cachea en memoria un access_token OAuth2.0 (client_credentials)
 * para que cuentas-service pueda llamar a clientes-service de forma segura.
 */
@Service
public class AuthTokenService {

    private final RestClient restClient = RestClient.create();
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String scope;

    private volatile String tokenCacheado;
    private volatile Instant expiraEn = Instant.EPOCH;

    public AuthTokenService(@Value("${bancoxyz.auth.token-uri}") String tokenUri,
                             @Value("${bancoxyz.auth.client-id}") String clientId,
                             @Value("${bancoxyz.auth.client-secret}") String clientSecret,
                             @Value("${bancoxyz.auth.scope}") String scope) {
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
    }

    public synchronized String obtenerToken() {
        if (tokenCacheado != null && Instant.now().isBefore(expiraEn)) {
            return tokenCacheado;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", scope);

        TokenResponse respuesta = restClient.post()
                .uri(tokenUri)
                .headers(h -> h.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        tokenCacheado = respuesta.access_token();
        long ttl = respuesta.expires_in() != null ? respuesta.expires_in() : 300;
        expiraEn = Instant.now().plusSeconds(Math.max(ttl - 30, 10));
        return tokenCacheado;
    }

    public HttpHeaders authHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(obtenerToken());
        return headers;
    }
}
