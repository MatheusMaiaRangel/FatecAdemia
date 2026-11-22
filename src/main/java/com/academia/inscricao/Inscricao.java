package com.academia.inscricao;

import com.academia.aula.Aula;
import com.academia.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "inscricoes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"aluno_id", "aula_id"})
})
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id", nullable = false)
    private Usuario aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aula_id", nullable = false)
    private Aula aula;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected Inscricao() {
    }

    public Inscricao(Usuario aluno, Aula aula, LocalDateTime criadaEm) {
        this.aluno = aluno;
        this.aula = aula;
        this.criadaEm = criadaEm;
    }

    public Long getId() {
        return id;
    }

    public Usuario getAluno() {
        return aluno;
    }

    public Aula getAula() {
        return aula;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }
}
