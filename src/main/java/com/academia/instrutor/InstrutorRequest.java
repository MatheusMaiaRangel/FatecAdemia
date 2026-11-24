package com.academia.instrutor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar ou substituir um instrutor")
public record InstrutorRequest(
        @NotBlank @Size(max = Instrutor.TAMANHO_MAXIMO_NOME)
        @Schema(example = "Carla Souza")
        String nome,

        @NotBlank @Size(max = Instrutor.TAMANHO_MAXIMO_ESPECIALIDADE)
        @Schema(example = "Pilates")
        String especialidade) {
}
