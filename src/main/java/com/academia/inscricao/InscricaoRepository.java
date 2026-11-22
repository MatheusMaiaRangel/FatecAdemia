package com.academia.inscricao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    List<Inscricao> findByAlunoIdOrderByCriadaEmDesc(Long alunoId);

    Optional<Inscricao> findByIdAndAlunoId(Long id, Long alunoId);

    boolean existsByAlunoIdAndAulaId(Long alunoId, Long aulaId);

    long countByAulaId(Long aulaId);
}
