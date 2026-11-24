package com.academia.modalidade;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criar ou substituir uma modalidade")
public record ModalidadeRequest(
        @NotBlank @Size(max = Modalidade.TAMANHO_MAXIMO_NOME)
        @Schema(example = "Pilates")
        String nome,

        @Size(max = Modalidade.TAMANHO_MAXIMO_DESCRICAO)
        @Schema(description = "Opcional", example = "Fortalecimento do core e melhora da postura")
        String descricao) {
}
