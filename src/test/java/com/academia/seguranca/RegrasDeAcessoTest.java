package com.academia.seguranca;

import com.academia.usuario.Papel;
import com.academia.usuario.Usuario;
import com.academia.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegrasDeAcessoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String tokenAluno;
    private String tokenAdmin;

    @BeforeEach
    void criarUsuarios() {
        tokenAluno = tokenDe(new Usuario("Aluno", "aluno.acesso@email.com", "hash", Papel.ALUNO));
        tokenAdmin = tokenDe(new Usuario("Admin", "admin.acesso@email.com", "hash", Papel.ADMIN));
    }

    @Test
    void semTokenDevolve401EmJson() throws Exception {
        mockMvc.perform(get("/aulas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(RespostaErroSeguranca.NAO_AUTENTICADO))
                .andExpect(jsonPath("$.caminho").value("/aulas"));
    }

    @Test
    void tokenInvalidoDevolve401() throws Exception {
        mockMvc.perform(get("/aulas").header(HttpHeaders.AUTHORIZATION, "Bearer token-falso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cabecalhoSemBearerDevolve401() throws Exception {
        mockMvc.perform(get("/aulas").header(HttpHeaders.AUTHORIZATION, tokenAluno))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeUsuarioQueNaoExisteMaisDevolve401() throws Exception {
        String tokenOrfao = tokenService.gerar(new Usuario("X", "sumiu@email.com", "hash", Papel.ALUNO));

        mockMvc.perform(get("/aulas").header(HttpHeaders.AUTHORIZATION, bearer(tokenOrfao)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void alunoPodeConsultarOCatalogo() throws Exception {
        for (String rota : new String[]{"/modalidades", "/instrutores", "/aulas/1"}) {
            mockMvc.perform(get(rota).header(HttpHeaders.AUTHORIZATION, bearer(tokenAluno)))
                    .andExpect(passouPelaSeguranca());
        }
    }

    @Test
    void alunoNaoPodeAlterarOCatalogoE403VemEmJson() throws Exception {
        mockMvc.perform(post("/modalidades").header(HttpHeaders.AUTHORIZATION, bearer(tokenAluno)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value(RespostaErroSeguranca.SEM_PERMISSAO));
        mockMvc.perform(delete("/aulas/1").header(HttpHeaders.AUTHORIZATION, bearer(tokenAluno)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPodeAlterarOCatalogo() throws Exception {
        mockMvc.perform(post("/instrutores").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(passouPelaSeguranca());
    }

    @Test
    void inscricoesSaoSoDoAluno() throws Exception {
        mockMvc.perform(get("/inscricoes").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/inscricoes").header(HttpHeaders.AUTHORIZATION, bearer(tokenAluno)))
                .andExpect(passouPelaSeguranca());
    }

    @Test
    void documentacaoDaApiEhPublica() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void raizLevaAoSwaggerSemPedirLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui.html"));
    }

    private String tokenDe(Usuario usuario) {
        return tokenService.gerar(usuarioRepository.save(usuario));
    }

    private static String bearer(String token) {
        return FiltroJwt.PREFIXO_BEARER + token;
    }

    // As rotas do catálogo ainda não existem; o que importa aqui é não ser barrado com 401 ou 403.
    private static ResultMatcher passouPelaSeguranca() {
        return resultado -> assertThat(resultado.getResponse().getStatus()).isNotIn(401, 403);
    }
}
