package com.academia.aula;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AulaRepository extends JpaRepository<Aula, Long> {

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
}
