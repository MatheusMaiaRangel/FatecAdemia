package com.academia.seguranca;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("jwt")
public record JwtProperties(
        @NotBlank String segredo,
        @NotNull Duration expiracao,
        @NotBlank String emissor) {
}
