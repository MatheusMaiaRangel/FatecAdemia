package com.academia.instrutor;

import com.academia.aula.Aula;
import com.academia.aula.AulaRepository;
import com.academia.modalidade.Modalidade;
import com.academia.modalidade.ModalidadeRepository;
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
class InstrutorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private InstrutorRepository instrutores;
    @Autowired private ModalidadeRepository modalidades;
    @Autowired private AulaRepository aulas;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private TokenService tokens;
    @Autowired private Clock relogio;

    private String adminToken;
    private String alunoToken;

    @BeforeEach
    void preparar() {
        Usuario admin = usuarios.save(new Usuario("Admin", "admin.instrutor@email.com", "hash", Papel.ADMIN));
        Usuario aluno = usuarios.save(new Usuario("Aluno", "aluno.instrutor@email.com", "hash", Papel.ALUNO));
        adminToken = "Bearer " + tokens.gerar(admin);
        alunoToken = "Bearer " + tokens.gerar(aluno);
    }

    @Test
    void adminExecutaCrudCompletoEAlunoConsulta() throws Exception {
        var criado = enviar(post("/instrutores"), new InstrutorRequest(" Carla ", " Pilates "), adminToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Carla"))
                .andExpect(jsonPath("$.especialidade").value("Pilates"))
                .andReturn().getResponse();
        long id = json.readTree(criado.getContentAsString()).get("id").asLong();
        assertThat(criado.getHeader(HttpHeaders.LOCATION)).endsWith("/instrutores/" + id);

        mockMvc.perform(get("/instrutores/" + id).header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carla"));
        enviar(put("/instrutores/" + id), new InstrutorRequest("Carla Souza", "Yoga"), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carla Souza"))
                .andExpect(jsonPath("$.especialidade").value("Yoga"));
        mockMvc.perform(delete("/instrutores/" + id).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNoContent());
        assertThat(instrutores.findById(id)).isEmpty();
    }

    @Test
    void listaEmOrdemAlfabetica() throws Exception {
        instrutores.save(new Instrutor("Diego", "Yoga"));
        instrutores.save(new Instrutor("Bruna", "Funcional"));
        mockMvc.perform(get("/instrutores").header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Bruna"))
                .andExpect(jsonPath("$[1].nome").value("Diego"));
    }

    @Test
    void naoExcluiInstrutorComAulas() throws Exception {
        Instrutor carla = instrutores.save(new Instrutor("Carla", "Pilates"));
        Modalidade pilates = modalidades.save(new Modalidade("Pilates", null));
        aulas.saveAndFlush(new Aula(LocalDateTime.now(relogio).minusDays(1), 10, pilates, carla));
        mockMvc.perform(delete("/instrutores/" + carla.getId()).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(InstrutorService.INSTRUTOR_COM_AULAS));
        assertThat(instrutores.findById(carla.getId())).isPresent();
    }

    @Test
    void validacaoDevolve400ComOsCampos() throws Exception {
        enviar(post("/instrutores"), new InstrutorRequest("", null), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(2));
        enviar(post("/instrutores"), new InstrutorRequest("x".repeat(101), "y".repeat(101)), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(2));
        mockMvc.perform(delete("/instrutores/-1").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void idInexistenteDevolve404() throws Exception {
        mockMvc.perform(get("/instrutores/999999").header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(InstrutorService.INSTRUTOR_NAO_ENCONTRADO));
        enviar(put("/instrutores/999999"), new InstrutorRequest("Carla", "Pilates"), adminToken)
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/instrutores/999999").header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void alunoRecebe403NasOperacoesDeEscrita() throws Exception {
        Instrutor carla = instrutores.save(new Instrutor("Carla", "Pilates"));
        enviar(post("/instrutores"), new InstrutorRequest("Diego", "Yoga"), alunoToken)
                .andExpect(status().isForbidden());
        enviar(put("/instrutores/" + carla.getId()), new InstrutorRequest("Diego", "Yoga"), alunoToken)
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/instrutores/" + carla.getId()).header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isForbidden());
        assertThat(instrutores.findById(carla.getId())).get().extracting(Instrutor::getNome).isEqualTo("Carla");
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mockMvc.perform(get("/instrutores")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/instrutores").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions enviar(MockHttpServletRequestBuilder requisicao, Object corpo, String token) throws Exception {
        return mockMvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)));
    }
}
