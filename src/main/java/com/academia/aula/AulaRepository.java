package com.academia.aula;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AulaRepository extends JpaRepository<Aula, Long> {

    // Sem EntityGraph: o bloqueio deve atingir a aula, não o catálogo associado.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Aula a where a.id = :id")
    Optional<Aula> buscarPorIdParaAtualizar(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"modalidade", "instrutor"})
    Page<Aula> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"modalidade", "instrutor"})
    Optional<Aula> findById(Long id);

    @EntityGraph(attributePaths = {"modalidade", "instrutor"})
    Page<Aula> findByHorarioAfterOrderByHorarioAsc(LocalDateTime horario, Pageable pageable);

    @EntityGraph(attributePaths = {"modalidade", "instrutor"})
    Page<Aula> findByModalidadeNomeContainingIgnoreCase(String nome, Pageable pageable);

    List<Aula> findByInstrutorIdAndHorarioBetween(Long instrutorId, LocalDateTime inicio, LocalDateTime fim);

    // Usadas pela Pessoa 2 antes de excluir modalidade ou instrutor (regra 4 e integridade do histórico).
    boolean existsByModalidadeIdAndHorarioAfter(Long modalidadeId, LocalDateTime horario);

    boolean existsByModalidadeId(Long modalidadeId);

    boolean existsByInstrutorId(Long instrutorId);
}
