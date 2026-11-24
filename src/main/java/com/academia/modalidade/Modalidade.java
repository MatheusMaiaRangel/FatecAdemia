package com.academia.modalidade;

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
@Table(name = "modalidades")
public class Modalidade {

    public static final int TAMANHO_MAXIMO_NOME = 100;
    public static final int TAMANHO_MAXIMO_DESCRICAO = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = TAMANHO_MAXIMO_NOME)
    private String nome;

    @Column(length = TAMANHO_MAXIMO_DESCRICAO)
    private String descricao;

    @OneToMany(mappedBy = "modalidade")
    private List<Aula> aulas = new ArrayList<>();

    protected Modalidade() {
    }

    public Modalidade(String nome, String descricao) {
        atualizar(nome, descricao);
    }

    public void atualizar(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<Aula> getAulas() {
        return Collections.unmodifiableList(aulas);
    }
}
