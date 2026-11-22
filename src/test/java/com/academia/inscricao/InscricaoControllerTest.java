package com.academia.inscricao;

import com.academia.aula.Aula;
import com.academia.aula.AulaRepository;
import com.academia.seguranca.TokenService;
import com.academia.usuario.Papel;
import com.academia.usuario.TokenResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InscricaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapeadorJson;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AulaRepository aulaRepository;

    @Autowired
    private InscricaoRepository inscricaoRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private Clock relogio;

    private Usuario ana;
    private Usuario bruno;
    private String tokenAna;
    private String tokenBruno;

    @BeforeEach
    void prepararDados() {
        ana = usuarioRepository.save(new Usuario("Ana", "ana.inscricao@email.com", "hash", Papel.ALUNO));
        bruno = usuarioRepository.save(new Usuario("Bruno", "bruno.inscricao@email.com", "hash", Papel.ALUNO));
        tokenAna = tokenService.gerar(ana);
        tokenBruno = tokenService.gerar(bruno);
    }

    @Test
    void alunoSeInscreveEmAulaComVagaEDevolve201() throws Exception {
        Aula aula = aulaRepository.save(new Aula(LocalDateTime.now(relogio).plusHours(4), 2));
        String corpo = mapeadorJson.writeValueAsString(new InscricaoRequest(aula.getId()));

        mockMvc.perform(post("/inscricoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAna))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.alunoId").value(ana.getId()))
                .andExpect(jsonPath("$.aulaId").value(aula.getId()));
    }

    @Test
    void naoPermiteSeInscreverEmAulaLotadaEDevolve409() throws Exception {
        Aula aulaComUmaVaga = aulaRepository.save(new Aula(LocalDateTime.now(relogio).plusHours(4), 1));
        inscricaoRepository.save(new Inscricao(ana, aulaComUmaVaga, LocalDateTime.now(relogio)));

        String corpo = mapeadorJson.writeValueAsString(new InscricaoRequest(aulaComUmaVaga.getId()));

        mockMvc.perform(post("/inscricoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenBruno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(InscricaoService.AULA_LOTADA));
    }

    @Test
    void verMinhasInscricoesSeparaPorDono() throws Exception {
        Aula aula1 = aulaRepository.save(new Aula(LocalDateTime.now(relogio).plusHours(3), 5));
        Aula aula2 = aulaRepository.save(new Aula(LocalDateTime.now(relogio).plusHours(5), 5));
        Inscricao inscricaoAna = inscricaoRepository.save(new Inscricao(ana, aula1, LocalDateTime.now(relogio)));
        inscricaoRepository.save(new Inscricao(bruno, aula2, LocalDateTime.now(relogio)));

        mockMvc.perform(get("/inscricoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(inscricaoAna.getId()))
                .andExpect(jsonPath("$[0].alunoId").value(ana.getId()))
                .andExpect(jsonPath("$[0].aulaId").value(aula1.getId()));
    }

    @Test
    void cancelaInscricaoQuandoFaltamMaisDeDuasHorasParaAAula() throws Exception {
        Aula aula = aulaRepository.save(new Aula(LocalDateTime.now(relogio).plusHours(3), 5));
        Inscricao inscricao = inscricaoRepository.save(new Inscricao(ana, aula, LocalDateTime.now(relogio)));

        mockMvc.perform(delete("/inscricoes/" + inscricao.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAna)))
                .andExpect(status().isNoContent());

        assertThat(inscricaoRepository.findById(inscricao.getId())).isEmpty();
    }

    @Test
    void naoPermiteCancelarComMenosDeDuasHorasParaAAulaEDevolve409() throws Exception {
        Aula aulaEmUmaHora = aulaRepository.save(new Aula(LocalDateTime.now(relogio).plusHours(1), 5));
        Inscricao inscricao = inscricaoRepository.save(new Inscricao(ana, aulaEmUmaHora, LocalDateTime.now(relogio)));

        mockMvc.perform(delete("/inscricoes/" + inscricao.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenAna)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(InscricaoService.PRAZO_CANCELAMENTO_ENCERRADO));

        assertThat(inscricaoRepository.findById(inscricao.getId())).isPresent();
    }

    @Test
    void alunoNaoPodeCancelarInscricaoDeOutroAluno() throws Exception {
        Aula aula = aulaRepository.save(new Aula(LocalDateTime.now(relogio).plusHours(4), 5));
        Inscricao inscricaoAna = inscricaoRepository.save(new Inscricao(ana, aula, LocalDateTime.now(relogio)));

        mockMvc.perform(delete("/inscricoes/" + inscricaoAna.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenBruno)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(InscricaoService.INSCRICAO_NAO_ENCONTRADA));

        assertThat(inscricaoRepository.findById(inscricaoAna.getId())).isPresent();
    }

    private static String bearer(String token) {
        return TokenResponse.TIPO_BEARER + " " + token;
    }
}
