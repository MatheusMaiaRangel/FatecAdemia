package com.academia.usuario;

public record TokenResponse(String token, String tipo) {

    public static final String TIPO_BEARER = "Bearer";

    public static TokenResponse bearer(String token) {
        return new TokenResponse(token, TIPO_BEARER);
    }
}
