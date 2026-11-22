package com.academia.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(security = @SecurityRequirement(name = SwaggerConfig.ESQUEMA_JWT))
@SecurityScheme(
        name = SwaggerConfig.ESQUEMA_JWT,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Cole aqui só o token devolvido pelo /auth/login, sem o prefixo Bearer.")
public class SwaggerConfig {

    static final String ESQUEMA_JWT = "bearerAuth";
}
