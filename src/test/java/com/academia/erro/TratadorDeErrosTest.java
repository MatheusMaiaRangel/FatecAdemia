package com.academia.erro;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TratadorDeErrosTest {

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerFalso())
                .setControllerAdvice(new TratadorDeErros())
                .build();
    }

    @Test
    void validacaoDevolve400ComOsCamposInvalidos() throws Exception {
        mockMvc.perform(post("/falso/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.DADOS_INVALIDOS))
                .andExpect(jsonPath("$.caminho").value("/falso/validacao"))
                .andExpect(jsonPath("$.campos[0].campo").value("nome"));
    }

    @Test
    void corpoMalFormatadoDevolve400() throws Exception {
        mockMvc.perform(post("/falso/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{nome"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.CORPO_INVALIDO));
    }

    @Test
    void parametroDeTipoErradoDevolve400() throws Exception {
        mockMvc.perform(get("/falso/numero/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.PARAMETRO_INVALIDO + "id"));
    }

    @Test
    void recursoNaoEncontradoDevolve404ComAMensagemDaExcecao() throws Exception {
        mockMvc.perform(get("/falso/nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Aula não encontrada"));
    }

    @Test
    void rotaInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/falso/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.ROTA_NAO_ENCONTRADA));
    }

    @Test
    void metodoNaoSuportadoDevolve405() throws Exception {
        mockMvc.perform(post("/falso/regra"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.METODO_NAO_SUPORTADO));
    }

    @Test
    void regraDeNegocioDevolve409ComAMensagemDaExcecao() throws Exception {
        mockMvc.perform(get("/falso/regra"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Conflict"))
                .andExpect(jsonPath("$.mensagem").value("Aula lotada"));
    }

    @Test
    void violacaoDeIntegridadeDevolve409SemExporOBanco() throws Exception {
        mockMvc.perform(get("/falso/integridade"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.CONFLITO_DE_DADOS));
    }

    @Test
    void credenciaisInvalidasDevolvem401() throws Exception {
        mockMvc.perform(get("/falso/credenciais"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(CredenciaisInvalidasException.MENSAGEM));
    }

    @Test
    void acessoNegadoDevolve403() throws Exception {
        mockMvc.perform(get("/falso/negado"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.ACESSO_NEGADO));
    }

    @Test
    void erroInesperadoDevolve500SemExporDetalhes() throws Exception {
        mockMvc.perform(get("/falso/erro"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.ERRO_INTERNO));
    }

    record DadosFalsos(@NotBlank String nome) {
    }

    @RestController
    static class ControllerFalso {

        @PostMapping("/falso/validacao")
        void validar(@Valid @RequestBody DadosFalsos dados) {
        }

        @GetMapping("/falso/numero/{id}")
        void numero(@PathVariable Long id) {
        }

        @GetMapping("/falso/nao-encontrado")
        void naoEncontrado() {
            throw new RecursoNaoEncontradoException("Aula não encontrada");
        }

        @GetMapping("/falso/regra")
        void regra() {
            throw new RegraNegocioException("Aula lotada");
        }

        @GetMapping("/falso/integridade")
        void integridade() {
            throw new DataIntegrityViolationException("Duplicate entry 'x' for key 'usuarios.email'");
        }

        @GetMapping("/falso/credenciais")
        void credenciais() {
            throw new CredenciaisInvalidasException();
        }

        @GetMapping("/falso/negado")
        void negado() {
            throw new AccessDeniedException("negado");
        }

        @GetMapping("/falso/erro")
        void erro() {
            throw new IllegalStateException("detalhe interno");
        }
    }
}
