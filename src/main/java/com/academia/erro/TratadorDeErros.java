package com.academia.erro;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class TratadorDeErros {

    static final String DADOS_INVALIDOS = "Dados inválidos";
    static final String CORPO_INVALIDO = "Corpo da requisição ausente ou mal formatado";
    static final String PARAMETRO_INVALIDO = "Parâmetro com valor inválido: ";
    static final String ROTA_NAO_ENCONTRADA = "Rota não encontrada";
    static final String METODO_NAO_SUPORTADO = "Método HTTP não suportado nesta rota";
    static final String ACESSO_NEGADO = "Acesso negado";
    static final String ERRO_INTERNO = "Erro interno no servidor";

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErro> tratarValidacao(MethodArgumentNotValidException ex,
                                                        HttpServletRequest requisicao) {
        List<RespostaErro.CampoInvalido> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new RespostaErro.CampoInvalido(erro.getField(), erro.getDefaultMessage()))
                .toList();
        RespostaErro corpo = RespostaErro.comCampos(HttpStatus.BAD_REQUEST, DADOS_INVALIDOS,
                requisicao.getRequestURI(), campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaErro> tratarCorpoInvalido(HttpServletRequest requisicao) {
        return responder(HttpStatus.BAD_REQUEST, CORPO_INVALIDO, requisicao);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespostaErro> tratarParametroInvalido(MethodArgumentTypeMismatchException ex,
                                                                HttpServletRequest requisicao) {
        return responder(HttpStatus.BAD_REQUEST, PARAMETRO_INVALIDO + ex.getName(), requisicao);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<RespostaErro> tratarNaoEncontrado(RecursoNaoEncontradoException ex,
                                                            HttpServletRequest requisicao) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), requisicao);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<RespostaErro> tratarRotaInexistente(HttpServletRequest requisicao) {
        return responder(HttpStatus.NOT_FOUND, ROTA_NAO_ENCONTRADA, requisicao);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RespostaErro> tratarMetodoNaoSuportado(HttpServletRequest requisicao) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, METODO_NAO_SUPORTADO, requisicao);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<RespostaErro> tratarRegraNegocio(RegraNegocioException ex,
                                                           HttpServletRequest requisicao) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), requisicao);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespostaErro> tratarAcessoNegado(HttpServletRequest requisicao) {
        return responder(HttpStatus.FORBIDDEN, ACESSO_NEGADO, requisicao);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErro> tratarErroInesperado(Exception ex, HttpServletRequest requisicao) {
        log.error("Erro inesperado em {} {}", requisicao.getMethod(), requisicao.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, ERRO_INTERNO, requisicao);
    }

    private ResponseEntity<RespostaErro> responder(HttpStatus status, String mensagem,
                                                   HttpServletRequest requisicao) {
        return ResponseEntity.status(status)
                .body(RespostaErro.de(status, mensagem, requisicao.getRequestURI()));
    }
}
