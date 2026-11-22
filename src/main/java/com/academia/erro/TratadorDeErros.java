package com.academia.erro;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class TratadorDeErros {

    static final String DADOS_INVALIDOS = "Dados inválidos";
    static final String CORPO_INVALIDO = "Corpo da requisição ausente ou mal formatado";
    static final String PARAMETRO_INVALIDO = "Parâmetro com valor inválido: ";
    static final String PARAMETRO_AUSENTE = "Parâmetro obrigatório ausente: ";
    static final String NAO_AUTENTICADO = "Autenticação necessária";
    static final String ROTA_NAO_ENCONTRADA = "Rota não encontrada";
    static final String METODO_NAO_SUPORTADO = "Método HTTP não suportado nesta rota";
    static final String TIPO_NAO_SUPORTADO = "Envie o corpo como application/json";
    static final String CONFLITO_DE_DADOS = "A operação conflita com dados já existentes";
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RespostaErro> tratarParametroAusente(MissingServletRequestParameterException ex,
                                                               HttpServletRequest requisicao) {
        return responder(HttpStatus.BAD_REQUEST, PARAMETRO_AUSENTE + ex.getParameterName(), requisicao);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<RespostaErro> tratarValidacaoDeParametros(HttpServletRequest requisicao) {
        return responder(HttpStatus.BAD_REQUEST, DADOS_INVALIDOS, requisicao);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespostaErro> tratarStatusExplicito(ResponseStatusException ex,
                                                              HttpServletRequest requisicao) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String mensagem = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return responder(status, mensagem, requisicao);
    }

    // Sem corpo: o cliente recusou JSON, então qualquer corpo de erro também seria recusado.
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> tratarFormatoNaoAceito() {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
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

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<RespostaErro> tratarTipoDeConteudoNaoSuportado(HttpServletRequest requisicao) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, TIPO_NAO_SUPORTADO, requisicao);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<RespostaErro> tratarRegraNegocio(RegraNegocioException ex,
                                                           HttpServletRequest requisicao) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), requisicao);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespostaErro> tratarViolacaoDeIntegridade(HttpServletRequest requisicao) {
        return responder(HttpStatus.CONFLICT, CONFLITO_DE_DADOS, requisicao);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<RespostaErro> tratarCredenciaisInvalidas(CredenciaisInvalidasException ex,
                                                                   HttpServletRequest requisicao) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), requisicao);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RespostaErro> tratarNaoAutenticado(HttpServletRequest requisicao) {
        return responder(HttpStatus.UNAUTHORIZED, NAO_AUTENTICADO, requisicao);
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
