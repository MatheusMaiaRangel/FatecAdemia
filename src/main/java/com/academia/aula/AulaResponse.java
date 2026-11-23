package com.academia.aula;

import java.time.LocalDateTime;

public record AulaResponse(Long id, LocalDateTime horario, int capacidade,
                           Long modalidadeId, String modalidadeNome,
                           Long instrutorId, String instrutorNome) {

    public static AulaResponse de(Aula aula) {
        return new AulaResponse(aula.getId(), aula.getHorario(), aula.getCapacidade(),
                aula.getModalidade().getId(), aula.getModalidade().getNome(),
                aula.getInstrutor().getId(), aula.getInstrutor().getNome());
    }
}
