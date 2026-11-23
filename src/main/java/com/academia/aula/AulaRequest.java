package com.academia.aula;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

@Schema(description = "Dados para criar ou substituir uma aula. Os IDs devem existir no catálogo.")
public record AulaRequest(
        @NotNull @Future
        @Schema(description = "Início da aula no horário local da academia", example = "2027-06-15T18:00:00")
        LocalDateTime horario,

        @NotNull @Positive
        @Schema(example = "20")
        Integer capacidade,

        @NotNull @Positive
        @Schema(example = "1")
        Long modalidadeId,

        @NotNull @Positive
        @Schema(example = "1")
        Long instrutorId) {
}
