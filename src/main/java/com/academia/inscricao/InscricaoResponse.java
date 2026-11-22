package com.academia.inscricao;

import java.time.LocalDateTime;

public record InscricaoResponse(
        Long id,
        Long alunoId,
        Long aulaId,
        LocalDateTime horarioAula,
        LocalDateTime criadaEm) {

    public static InscricaoResponse de(Inscricao inscricao) {
        return new InscricaoResponse(
                inscricao.getId(),
                inscricao.getAluno().getId(),
                inscricao.getAula().getId(),
                inscricao.getAula().getHorario(),
                inscricao.getCriadaEm());
    }
}
