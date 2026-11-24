package com.academia.instrutor;

import com.academia.aula.Aula;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "instrutores")
public class Instrutor {

    public static final int TAMANHO_MAXIMO_NOME = 100;
    public static final int TAMANHO_MAXIMO_ESPECIALIDADE = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = TAMANHO_MAXIMO_NOME)
    private String nome;

    @Column(nullable = false, length = TAMANHO_MAXIMO_ESPECIALIDADE)
    private String especialidade;

    @OneToMany(mappedBy = "instrutor")
    private List<Aula> aulas = new ArrayList<>();

    protected Instrutor() {
    }

    public Instrutor(String nome, String especialidade) {
        atualizar(nome, especialidade);
    }

    public void atualizar(String nome, String especialidade) {
        this.nome = nome;
        this.especialidade = especialidade;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public List<Aula> getAulas() {
        return Collections.unmodifiableList(aulas);
    }
}
