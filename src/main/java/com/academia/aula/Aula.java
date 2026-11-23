package com.academia.aula;

import com.academia.inscricao.Inscricao;
import com.academia.instrutor.Instrutor;
import com.academia.modalidade.Modalidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "aulas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_aula_instrutor_horario", columnNames = {"instrutor_id", "horario"})
})
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime horario;

    @Column(nullable = false)
    private int capacidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modalidade_id", nullable = false)
    private Modalidade modalidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrutor_id", nullable = false)
    private Instrutor instrutor;

    @OneToMany(mappedBy = "aula")
    private List<Inscricao> inscricoes = new ArrayList<>();

    protected Aula() {
    }

    public Aula(LocalDateTime horario, int capacidade, Modalidade modalidade, Instrutor instrutor) {
        atualizar(horario, capacidade, modalidade, instrutor);
    }

    public void atualizar(LocalDateTime horario, int capacidade, Modalidade modalidade, Instrutor instrutor) {
        this.horario = horario;
        this.capacidade = capacidade;
        this.modalidade = modalidade;
        this.instrutor = instrutor;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getHorario() {
        return horario;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    public Instrutor getInstrutor() {
        return instrutor;
    }

    public List<Inscricao> getInscricoes() {
        return Collections.unmodifiableList(inscricoes);
    }
}
