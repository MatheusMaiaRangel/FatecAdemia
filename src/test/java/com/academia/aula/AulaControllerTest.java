package com.academia.aula;

import com.academia.inscricao.Inscricao;
import com.academia.inscricao.InscricaoRepository;
import com.academia.instrutor.Instrutor;
import com.academia.instrutor.InstrutorRepository;
import com.academia.modalidade.Modalidade;
import com.academia.modalidade.ModalidadeRepository;
import com.academia.seguranca.TokenService;
import com.academia.usuario.Papel;
import com.academia.usuario.Usuario;
import com.academia.usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AulaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private AulaRepository aulas;
    @Autowired private ModalidadeRepository modalidades;
    @Autowired private InstrutorRepository instrutores;
    @Autowired private InscricaoRepository inscricoes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private TokenService tokens;
    @Autowired private Clock relogio;
    @Autowired private EntityManager em;

    private Modalidade pilates;
    private Modalidade yoga;
    private Instrutor carla;
    private Instrutor diego;
    private Usuario aluno;
    private String adminToken;
    private String alunoToken;
    private LocalDateTime horario;

    @BeforeEach
    void preparar() {
        pilates = modalidades.save(new Modalidade("Pilates", "Fortalecimento e postura"));
        yoga = modalidades.save(new Modalidade("Yoga", "Respiração e equilíbrio"));
        carla = instrutores.save(new Instrutor("Carla", "Pilates"));
        diego = instrutores.save(new Instrutor("Diego", "Yoga"));
        aluno = usuarios.save(new Usuario("Aluno", "aluno.aula@email.com", "hash", Papel.ALUNO));
        Usuario admin = usuarios.save(new Usuario("Admin", "admin.aula@email.com", "hash", Papel.ADMIN));
        adminToken = "Bearer " + tokens.gerar(admin);
        alunoToken = "Bearer " + tokens.gerar(aluno);
        horario = LocalDateTime.now(relogio).plusDays(5).withHour(18).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void adminExecutaCrudCompletoEAlunoConsulta() throws Exception {
        String resposta = enviar(post("/aulas"), dados(horario, 20), adminToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modalidadeNome").value("Pilates"))
                .andExpect(jsonPath("$.instrutorNome").value("Carla"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(resposta).get("id").asLong();

        mockMvc.perform(get("/aulas/" + id).header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacidade").value(20));
        enviar(put("/aulas/" + id), dados(horario.plusHours(1), 30), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacidade").value(30));
        mockMvc.perform(delete("/aulas/" + id).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNoContent());
        assertThat(aulas.findById(id)).isEmpty();
    }

    @Test
    void criacaoDevolveLocationQuePermiteConsultarAAula() throws Exception {
        var resultado = enviar(post("/aulas"), dados(horario, 20), adminToken)
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn();
        String local = resultado.getResponse().getHeader(HttpHeaders.LOCATION);
        mockMvc.perform(get(local).header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horario").value(horario.toString() + ":00"));
    }

    @Test
    void listaComPaginacaoOrdenacaoEFiltroSemDiferenciarMaiusculas() throws Exception {
        salvar(horario, 10, pilates, carla);
        Aula segunda = salvar(horario.plusHours(1), 30, pilates, carla);
        salvar(horario.plusHours(2), 50, yoga, diego);
        mockMvc.perform(get("/aulas").param("modalidade", "  PILA  ")
                        .param("page", "0").param("size", "1").param("sort", "capacidade,desc")
                        .header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(segunda.getId()))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(1));
        mockMvc.perform(get("/aulas").param("size", "1").param("page", "1")
                        .header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(segunda.getId()))
                .andExpect(jsonPath("$.totalElementos").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"id,asc", "modalidade.nome,desc", "instrutor.nome,asc", "horario,DESC"})
    void aceitaOrdenacoesDocumentadas(String sort) throws Exception {
        salvar(horario, 10, pilates, carla);
        mockMvc.perform(get("/aulas").param("sort", sort).header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void listaSomenteAulasFuturasEmOrdemCrescenteComPaginacao() throws Exception {
        salvar(LocalDateTime.now(relogio).minusDays(1), 10, pilates, carla);
        salvar(horario.plusDays(1), 10, pilates, carla);
        Aula proxima = salvar(horario, 10, yoga, diego);
        mockMvc.perform(get("/aulas/proximas").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].id").value(proxima.getId()))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void buscaSemResultadoDevolvePaginaVazia() throws Exception {
        mockMvc.perform(get("/aulas").param("modalidade", "inexistente")
                        .header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void criarComMesmoInstrutorEHorarioDevolve409() throws Exception {
        salvar(horario, 10, pilates, carla);
        enviar(post("/aulas"), dados(horario, 10), adminToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(AulaService.CONFLITO_DE_HORARIO));
        assertThat(aulas.count()).isEqualTo(1);
    }

    @Test
    void bancoTambemImpedeDuasAulasDoInstrutorNoMesmoHorario() {
        salvar(horario, 10, pilates, carla);
        assertThatThrownBy(() -> salvar(horario, 20, yoga, carla))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void permiteMesmoHorarioParaInstrutoresDiferentes() throws Exception {
        salvar(horario, 10, pilates, carla);
        AulaRequest outra = new AulaRequest(horario, 10, yoga.getId(), diego.getId());
        enviar(post("/aulas"), outra, adminToken).andExpect(status().isCreated());
    }

    @Test
    void permiteOutroHorarioDoMesmoInstrutorNoMesmoDia() throws Exception {
        salvar(horario.minusHours(1), 10, pilates, carla);
        enviar(post("/aulas"), dados(horario, 10), adminToken).andExpect(status().isCreated());
    }

    @Test
    void editarSemMudarHorarioNaoConflitaComAPropriaAula() throws Exception {
        Aula aula = salvar(horario, 10, pilates, carla);
        enviar(put("/aulas/" + aula.getId()), dados(horario, 20), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacidade").value(20));
    }

    @Test
    void editarParaHorarioOcupadoDevolve409ESemAlterarRegistro() throws Exception {
        salvar(horario, 10, pilates, carla);
        Aula aula = salvar(horario.plusHours(1), 10, pilates, carla);
        enviar(put("/aulas/" + aula.getId()), dados(horario, 20), adminToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(AulaService.CONFLITO_DE_HORARIO));
        assertThat(aula.getHorario()).isEqualTo(horario.plusHours(1));
        assertThat(aula.getCapacidade()).isEqualTo(10);
    }

    @Test
    void trocarInstrutorTambemVerificaConflito() throws Exception {
        salvar(horario, 10, pilates, carla);
        Aula aula = salvar(horario, 10, yoga, diego);
        enviar(put("/aulas/" + aula.getId()), dados(horario, 20), adminToken)
                .andExpect(status().isConflict());
    }

    @Test
    void naoReduzCapacidadeAbaixoDoNumeroDeInscritos() throws Exception {
        Aula aula = salvar(horario, 10, pilates, carla);
        inscrever(aluno, aula);
        Usuario outro = usuarios.save(new Usuario("Outro", "outro.aula@email.com", "hash", Papel.ALUNO));
        inscrever(outro, aula);
        enviar(put("/aulas/" + aula.getId()), dados(horario, 1), adminToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(AulaService.CAPACIDADE_INSUFICIENTE));
        assertThat(aula.getCapacidade()).isEqualTo(10);
    }

    @Test
    void permiteCapacidadeIgualAoNumeroDeInscritos() throws Exception {
        Aula aula = salvar(horario, 10, pilates, carla);
        inscrever(aluno, aula);
        enviar(put("/aulas/" + aula.getId()), dados(horario, 1), adminToken)
                .andExpect(status().isOk());
    }

    @Test
    void naoExcluiAulaComInscricoes() throws Exception {
        Aula aula = salvar(horario, 10, pilates, carla);
        inscrever(aluno, aula);
        mockMvc.perform(delete("/aulas/" + aula.getId()).header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(AulaService.AULA_COM_INSCRICOES));
        assertThat(aulas.findById(aula.getId())).isPresent();
        assertThat(inscricoes.countByAulaId(aula.getId())).isEqualTo(1);
    }

    @Test
    void idsInexistentesDevolvem404() throws Exception {
        long ausente = Long.MAX_VALUE;
        for (var requisicao : new MockHttpServletRequestBuilder[]{get("/aulas/" + ausente), delete("/aulas/" + ausente)}) {
            mockMvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, adminToken))
                    .andExpect(status().isNotFound());
        }
        enviar(put("/aulas/" + ausente), dados(horario, 10), adminToken).andExpect(status().isNotFound());
        enviar(post("/aulas"), new AulaRequest(horario, 10, ausente, carla.getId()), adminToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(AulaService.MODALIDADE_NAO_ENCONTRADA));
        enviar(post("/aulas"), new AulaRequest(horario, 10, pilates.getId(), ausente), adminToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(AulaService.INSTRUTOR_NAO_ENCONTRADO));
    }

    @Test
    void beanValidationExigeCamposHorarioFuturoCapacidadeEIdsPositivos() throws Exception {
        enviar(post("/aulas"), Map.of(), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(4));
        enviar(post("/aulas"), new AulaRequest(LocalDateTime.now(relogio).minusDays(1), 0, -1L, 0L), adminToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(4));
        Aula aula = salvar(horario, 10, pilates, carla);
        enviar(put("/aulas/" + aula.getId()), Map.of(), adminToken).andExpect(status().isBadRequest());
        assertThat(aula.getCapacidade()).isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"horario", "nome,asc", "horario,errado", "horario,asc,extra", "", "horario,", ",asc"})
    void ordenacaoInvalidaDevolve400(String sort) throws Exception {
        mockMvc.perform(get("/aulas").param("sort", sort).header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc", "modalidade=", "modalidade=   "})
    void parametrosInvalidosDevolvem400(String parametro) throws Exception {
        String[] partes = parametro.split("=", 2);
        mockMvc.perform(get("/aulas").param(partes[0], partes[1])
                        .header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void proximasValidaPaginacaoEIdDeveSerPositivo() throws Exception {
        mockMvc.perform(get("/aulas/proximas").param("size", "0")
                        .header(HttpHeaders.AUTHORIZATION, alunoToken)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/aulas/-1").header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alunoRecebe403NasOperacoesDeEscrita() throws Exception {
        Aula aula = salvar(horario, 10, pilates, carla);
        enviar(post("/aulas"), dados(horario.plusHours(1), 10), alunoToken).andExpect(status().isForbidden());
        enviar(put("/aulas/" + aula.getId()), dados(horario, 20), alunoToken).andExpect(status().isForbidden());
        mockMvc.perform(delete("/aulas/" + aula.getId()).header(HttpHeaders.AUTHORIZATION, alunoToken))
                .andExpect(status().isForbidden());
        assertThat(aulas.count()).isEqualTo(1);
        assertThat(aula.getCapacidade()).isEqualTo(10);
    }

    @Test
    void semTokenRecebe401() throws Exception {
        mockMvc.perform(get("/aulas")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/aulas").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(dados(horario, 10))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerDocumentaTodosOsEndpointsEAuthorize() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/aulas'].get.summary").isNotEmpty())
                .andExpect(jsonPath("$.paths['/aulas'].post.summary").isNotEmpty())
                .andExpect(jsonPath("$.paths['/aulas/proximas'].get.summary").isNotEmpty())
                .andExpect(jsonPath("$.paths['/aulas/{id}'].get.summary").isNotEmpty())
                .andExpect(jsonPath("$.paths['/aulas/{id}'].put.summary").isNotEmpty())
                .andExpect(jsonPath("$.paths['/aulas/{id}'].delete.summary").isNotEmpty());
    }

    @Test
    void consultaDeAgendaRespeitaInstrutorELimitesInclusivosDoIntervalo() {
        Aula inicio = salvar(horario, 10, pilates, carla);
        Aula fim = salvar(horario.plusHours(1), 10, pilates, carla);
        salvar(horario.plusDays(1), 10, pilates, carla);
        salvar(horario, 10, yoga, diego);
        assertThat(aulas.findByInstrutorIdAndHorarioBetween(carla.getId(), horario, horario.plusHours(1)))
                .extracting(Aula::getId).containsExactlyInAnyOrder(inicio.getId(), fim.getId());
    }

    @Test
    void relacionamentosSaoPersistidosSemExporEntidadesNasRespostas() {
        Aula aula = salvar(horario, 10, pilates, carla);
        Inscricao inscricao = inscrever(aluno, aula);
        Long id = aula.getId();
        Long modalidadeId = pilates.getId();
        Long instrutorId = carla.getId();
        em.flush();
        em.clear();
        Aula recarregada = aulas.findById(id).orElseThrow();
        assertThat(recarregada.getInscricoes()).extracting(Inscricao::getId).containsExactly(inscricao.getId());
        Modalidade modalidade = modalidades.findById(modalidadeId).orElseThrow();
        Instrutor instrutor = instrutores.findById(instrutorId).orElseThrow();
        assertThat(modalidade.getAulas()).extracting(Aula::getId).containsExactly(id);
        assertThat(modalidade.getDescricao()).isEqualTo("Fortalecimento e postura");
        assertThat(instrutor.getAulas()).extracting(Aula::getId).containsExactly(id);
        assertThat(instrutor.getEspecialidade()).isEqualTo("Pilates");
    }

    private AulaRequest dados(LocalDateTime inicio, int capacidade) {
        return new AulaRequest(inicio, capacidade, pilates.getId(), carla.getId());
    }

    private Aula salvar(LocalDateTime inicio, int capacidade, Modalidade modalidade, Instrutor instrutor) {
        return aulas.saveAndFlush(new Aula(inicio, capacidade, modalidade, instrutor));
    }

    private Inscricao inscrever(Usuario usuario, Aula aula) {
        return inscricoes.saveAndFlush(new Inscricao(usuario, aula, LocalDateTime.now(relogio)));
    }

    private ResultActions enviar(MockHttpServletRequestBuilder requisicao, Object corpo, String token) throws Exception {
        return mockMvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)));
    }
}
