package com.academia.inscricao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InscricaoRequest(
        @NotNull
        @Positive
        Long aulaId) {
}
