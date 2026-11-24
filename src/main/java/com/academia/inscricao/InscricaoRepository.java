package com.academia.inscricao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    List<Inscricao> findByAlunoIdOrderByCriadaEmDesc(Long alunoId);

    Optional<Inscricao> findByIdAndAlunoId(Long id, Long alunoId);

    boolean existsByAlunoIdAndAulaId(Long alunoId, Long aulaId);

    boolean existsByAlunoIdAndAulaHorarioAndAulaIdNot(Long alunoId, LocalDateTime horario, Long aulaId);

    @Query("select i.aluno.id from Inscricao i where i.aula.id = :aulaId order by i.aluno.id")
    List<Long> buscarAlunoIdsPorAula(@Param("aulaId") Long aulaId);

    long countByAulaId(Long aulaId);
}
