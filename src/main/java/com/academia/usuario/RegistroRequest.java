package com.academia.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank
        @Size(max = LimitesUsuario.TAMANHO_MAXIMO_NOME)
        String nome,

        @NotBlank
        @Email
        @Size(max = LimitesUsuario.TAMANHO_MAXIMO_EMAIL)
        String email,

        @NotBlank
        @Size(min = LimitesUsuario.TAMANHO_MINIMO_SENHA)
        @CabeNoBcrypt
        String senha) {
}
