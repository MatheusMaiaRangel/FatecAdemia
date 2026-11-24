package com.academia.erro;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Set;
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
    void campoComTipoErradoDizQualCampoEOQueEsperava() throws Exception {
        mockMvc.perform(post("/falso/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Ana\", \"capacidade\": \"vinte\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.DADOS_INVALIDOS))
                .andExpect(jsonPath("$.campos[0].campo").value("capacidade"))
                .andExpect(jsonPath("$.campos[0].mensagem").value(TratadorDeErros.DEVE_SER_NUMERO));
    }

    @Test
    void dataEmFormatoErradoMostraOFormatoEsperado() throws Exception {
        mockMvc.perform(post("/falso/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Ana\", \"horario\": \"15/06/2027 18:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("horario"))
                .andExpect(jsonPath("$.campos[0].mensagem").value(TratadorDeErros.FORMATO_DATA_HORA));
    }

    @Test
    void campoComTipoErradoDeOutroTipoAindaApontaOCampo() throws Exception {
        mockMvc.perform(post("/falso/validacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": {\"x\": 1}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("nome"))
                .andExpect(jsonPath("$.campos[0].mensagem").value(TratadorDeErros.FORMATO_INVALIDO));
    }

    @Test
    void corpoQueNaoEhJsonDevolve415() throws Exception {
        mockMvc.perform(post("/falso/validacao")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("nome=Ana"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.TIPO_NAO_SUPORTADO));
    }

    @Test
    void parametroObrigatorioAusenteDevolve400() throws Exception {
        mockMvc.perform(get("/falso/busca"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.PARAMETRO_AUSENTE + "termo"));
    }

    @Test
    void validacaoEmParametroDaRotaDevolve400() throws Exception {
        mockMvc.perform(get("/falso/positivo/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.DADOS_INVALIDOS));
    }

    @Test
    void violacaoDeRestricaoLancadaNoServicoDevolve400() throws Exception {
        mockMvc.perform(get("/falso/restricao"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.DADOS_INVALIDOS));
    }

    @Test
    void responseStatusExceptionMantemOStatusEAMensagem() throws Exception {
        mockMvc.perform(get("/falso/status"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.mensagem").value("Aula removida"));
        mockMvc.perform(get("/falso/status-sem-motivo"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.mensagem").value("Gone"));
    }

    @Test
    void formatoDeRespostaNaoAceitoDevolve406() throws Exception {
        mockMvc.perform(get("/falso/json").accept(MediaType.IMAGE_PNG))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void falhaDeAutenticacaoDentroDoMetodoDevolve401() throws Exception {
        mockMvc.perform(get("/falso/sem-autenticacao"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.NAO_AUTENTICADO));
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
    void violacaoDeRestricaoConhecidaExplicaACausa() throws Exception {
        mockMvc.perform(get("/falso/integridade-conhecida"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("O instrutor já tem uma aula neste horário"));
    }

    @Test
    void registroTravadoPorOutraOperacaoDevolve409PedindoNovaTentativa() throws Exception {
        mockMvc.perform(get("/falso/travado"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(TratadorDeErros.REGISTRO_EM_USO));
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

    record DadosFalsos(@NotBlank String nome, Integer capacidade, LocalDateTime horario) {
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

        @GetMapping("/falso/busca")
        void busca(@RequestParam String termo) {
        }

        @GetMapping("/falso/positivo/{numero}")
        void positivo(@PathVariable @Positive Integer numero) {
        }

        @GetMapping("/falso/restricao")
        void restricao() {
            throw new ConstraintViolationException("inválido", Set.of());
        }

        @GetMapping("/falso/status")
        void statusComMotivo() {
            throw new ResponseStatusException(HttpStatus.GONE, "Aula removida");
        }

        @GetMapping("/falso/status-sem-motivo")
        void statusSemMotivo() {
            throw new ResponseStatusException(HttpStatus.GONE);
        }

        @GetMapping(value = "/falso/json", produces = MediaType.APPLICATION_JSON_VALUE)
        String json() {
            return "{}";
        }

        @GetMapping("/falso/sem-autenticacao")
        void semAutenticacao() {
            throw new AuthenticationCredentialsNotFoundException("sem credenciais");
        }

        @GetMapping("/falso/integridade")
        void integridade() {
            throw new DataIntegrityViolationException("Duplicate entry 'x' for key 'usuarios.email'");
        }

        @GetMapping("/falso/integridade-conhecida")
        void integridadeConhecida() {
            throw new DataIntegrityViolationException("could not execute statement", new IllegalStateException(
                    "Duplicate entry '3-2027-06-15 18:00:00' for key 'aulas.uk_aula_instrutor_horario'"));
        }

        @GetMapping("/falso/travado")
        void travado() {
            throw new CannotAcquireLockException("Lock wait timeout exceeded");
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
