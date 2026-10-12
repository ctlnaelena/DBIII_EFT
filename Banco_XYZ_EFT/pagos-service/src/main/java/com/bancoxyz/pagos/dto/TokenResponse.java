package com.bancoxyz.pagos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenResponse(String access_token, String token_type, Long expires_in) {
}
