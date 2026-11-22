package com.academia.erro;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RespostaErro(
        Instant instante,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<CampoInvalido> campos) {

    public record CampoInvalido(String campo, String mensagem) {
    }

    public static RespostaErro de(HttpStatus status, String mensagem, String caminho) {
        return comCampos(status, mensagem, caminho, List.of());
    }

    public static RespostaErro comCampos(HttpStatus status, String mensagem, String caminho,
                                         List<CampoInvalido> campos) {
        return new RespostaErro(Instant.now(), status.value(), status.getReasonPhrase(),
                mensagem, caminho, List.copyOf(campos));
    }
}
