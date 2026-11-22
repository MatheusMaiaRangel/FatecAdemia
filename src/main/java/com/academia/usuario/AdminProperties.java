package com.academia.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("admin")
public record AdminProperties(
        @NotBlank
        @Size(max = LimitesUsuario.TAMANHO_MAXIMO_NOME)
        String nome,

        @NotBlank
        @Email
        @Size(max = LimitesUsuario.TAMANHO_MAXIMO_EMAIL)
        String email,

        @NotBlank
        @Size(min = LimitesUsuario.TAMANHO_MINIMO_SENHA, max = LimitesUsuario.TAMANHO_MAXIMO_SENHA)
        String senha) {

    @Override
    public String toString() {
        return "AdminProperties[nome=" + nome + ", email=" + email + ", senha=***]";
    }
}
