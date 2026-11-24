package com.academia.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_email", columnNames = "email")
})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = LimitesUsuario.TAMANHO_MAXIMO_NOME)
    private String nome;

    @Column(nullable = false, length = LimitesUsuario.TAMANHO_MAXIMO_EMAIL)
    private String email;

    @Column(nullable = false, length = LimitesUsuario.TAMANHO_HASH_SENHA)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = LimitesUsuario.TAMANHO_MAXIMO_PAPEL)
    private Papel papel;

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senhaCriptografada, Papel papel) {
        this.nome = nome;
        this.email = email;
        this.senha = senhaCriptografada;
        this.papel = papel;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public Papel getPapel() {
        return papel;
    }
}
