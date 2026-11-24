package com.academia.modalidade;

import com.academia.aula.Aula;
import com.academia.aula.AulaRepository;
import com.academia.instrutor.Instrutor;
import com.academia.instrutor.InstrutorRepository;
import com.academia.seguranca.TokenService;
import com.academia.usuario.Papel;
import com.academia.usuario.Usuario;
import com.academia.usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ModalidadeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private ModalidadeRepository modalidades;
    @Autowired private InstrutorRepository instrutores;
    @Autowired private AulaRepository aulas;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private TokenService tokens;
    @Autowired private Clock relogio;

    private String adminToken;
    private String alunoToken;

    @BeforeEach
    void preparar() {
        Usuario admin = usuarios.save(new Usuario("Admin", "admin.modalidade@email.com", "hash", Papel.ADMIN));
        Usuario aluno = usuarios.save(new Usuario("Aluno", "aluno.modalidade@email.com", "hash", Papel.ALUNO));
        adminToken = "Bearer " + tokens.gerar(admin);
        alunoToken = "Bearer " + tokens.gerar(aluno);
    }

    @Test
    void adminExecutaCrudCompletoEAlunoConsulta() throws Exception {
        var criada = enviar(post("/modalidades"), new ModalidadeRequest("  Pilates ", " Postura "), adminToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Pilates"))
                .andExpect(jsonPath("$.descricao").value("Postura"))
                .andReturn().getResponse();
        long id = json.readTree(criada.getContentAsString()).get("id").asLong();
        assertThat(criada.getHeader(HttpHeaders.LOCATION)).endsWith("/modalidades/" + id);

        mockMvc.perform(get("/modalidades/" + id).header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Pilates"));
        enviar(put("/modalidades/" + id), new ModalidadeRequest("Pilates Solo", null), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Pilates Solo"))
                .andExpect(jsonPath("$.descricao").doesNotExist());
        mockMvc.perform(delete("/modalidades/" + id).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNoContent());
        assertThat(modalidades.findById(id)).isEmpty();
    }

    @Test
    void listaEmOrdemAlfabetica() throws Exception {
        modalidades.save(new Modalidade("Yoga", null));
        modalidades.save(new Modalidade("Funcional", "Circuito"));
        mockMvc.perform(get("/modalidades").header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Funcional"))
                .andExpect(jsonPath("$[1].nome").value("Yoga"));
    }

    @Test
    void descricaoEmBrancoViraNula() throws Exception {
        enviar(post("/modalidades"), new ModalidadeRequest("Spinning", "   "), adminToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").doesNotExist());
    }

    @Test
    void regra4NaoExcluiModalidadeComAulaFutura() throws Exception {
        Modalidade pilates = modalidades.save(new Modalidade("Pilates", null));
        salvarAula(pilates, LocalDateTime.now(relogio).plusDays(1));
        mockMvc.perform(delete("/modalidades/" + pilates.getId()).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(ModalidadeService.MODALIDADE_COM_AULAS_FUTURAS));
        assertThat(modalidades.findById(pilates.getId())).isPresent();
    }

    @Test
    void naoExcluiModalidadeComAulasNoHistorico() throws Exception {
        Modalidade pilates = modalidades.save(new Modalidade("Pilates", null));
        salvarAula(pilates, LocalDateTime.now(relogio).minusDays(1));
        mockMvc.perform(delete("/modalidades/" + pilates.getId()).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(ModalidadeService.MODALIDADE_COM_HISTORICO));
    }

    @Test
    void validacaoDevolve400ComOsCampos() throws Exception {
        enviar(post("/modalidades"), new ModalidadeRequest(" ", "x".repeat(501)), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(2));
        enviar(post("/modalidades"), new ModalidadeRequest("x".repeat(101), null), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("nome"));
        mockMvc.perform(get("/modalidades/0").header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void idInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/modalidades/999999").header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(ModalidadeService.MODALIDADE_NAO_ENCONTRADA));
        enviar(put("/modalidades/999999"), new ModalidadeRequest("Yoga", null), adminToken)
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/modalidades/999999").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void alunoRecebe403NasOperacoesDeEscrita() throws Exception {
        Modalidade yoga = modalidades.save(new Modalidade("Yoga", null));
        enviar(post("/modalidades"), new ModalidadeRequest("Boxe", null), alunoToken)
                .andExpect(status().isForbidden());
        enviar(put("/modalidades/" + yoga.getId()), new ModalidadeRequest("Boxe", null), alunoToken)
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/modalidades/" + yoga.getId()).header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isForbidden());
        assertThat(modalidades.findById(yoga.getId())).get().extracting(Modalidade::getNome).isEqualTo("Yoga");
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mockMvc.perform(get("/modalidades")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/modalidades").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nome", "Boxe"))))
                .andExpect(status().isUnauthorized());
    }

    private void salvarAula(Modalidade modalidade, LocalDateTime horario) {
        Instrutor instrutor = instrutores.save(new Instrutor("Carla", "Pilates"));
        aulas.saveAndFlush(new Aula(horario, 10, modalidade, instrutor));
    }

    private ResultActions enviar(MockHttpServletRequestBuilder requisicao, Object corpo, String token) throws Exception {
        return mockMvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)));
    }
}
