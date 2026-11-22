package com.academia.seguranca;

import com.academia.erro.RespostaErro;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RespostaErroSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String NAO_AUTENTICADO = "Autenticação necessária: envie um token válido";
    static final String SEM_PERMISSAO = "Seu papel não tem permissão para acessar este recurso";

    private final ObjectMapper mapeadorJson;

    public RespostaErroSeguranca(ObjectMapper mapeadorJson) {
        this.mapeadorJson = mapeadorJson;
    }

    @Override
    public void commence(HttpServletRequest requisicao, HttpServletResponse resposta,
                         AuthenticationException excecao) throws IOException {
        escrever(resposta, HttpStatus.UNAUTHORIZED, NAO_AUTENTICADO, requisicao);
    }

    @Override
    public void handle(HttpServletRequest requisicao, HttpServletResponse resposta,
                       AccessDeniedException excecao) throws IOException {
        escrever(resposta, HttpStatus.FORBIDDEN, SEM_PERMISSAO, requisicao);
    }

    private void escrever(HttpServletResponse resposta, HttpStatus status, String mensagem,
                          HttpServletRequest requisicao) throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapeadorJson.writeValue(resposta.getOutputStream(),
                RespostaErro.de(status, mensagem, requisicao.getRequestURI()));
    }
}
