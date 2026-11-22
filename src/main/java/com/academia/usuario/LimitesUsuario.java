package com.academia.usuario;

public final class LimitesUsuario {

    public static final int TAMANHO_MAXIMO_NOME = 100;
    public static final int TAMANHO_MAXIMO_EMAIL = 150;
    public static final int TAMANHO_MINIMO_SENHA = 8;
    // O BCrypt ignora tudo depois do 72º byte, então senhas maiores dariam falsa sensação de segurança.
    public static final int TAMANHO_MAXIMO_SENHA_EM_BYTES = 72;
    public static final int TAMANHO_HASH_SENHA = 60;
    public static final int TAMANHO_MAXIMO_PAPEL = 20;

    private LimitesUsuario() {
    }
}
