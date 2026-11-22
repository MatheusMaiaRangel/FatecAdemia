package com.academia.erro;

public class CredenciaisInvalidasException extends RuntimeException {

    // Mesma mensagem para e-mail inexistente e senha errada, para não revelar quais e-mails estão cadastrados.
    public static final String MENSAGEM = "E-mail ou senha inválidos";

    public CredenciaisInvalidasException() {
        super(MENSAGEM);
    }
}
