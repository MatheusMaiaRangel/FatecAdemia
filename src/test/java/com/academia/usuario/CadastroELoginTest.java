package com.academia.usuario;

import com.academia.erro.CredenciaisInvalidasException;
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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CadastroELoginTest {

    private static final String SENHA = "senha-forte-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapeadorJson;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdminProperties admin;

    private String tokenAdmin;

    @BeforeEach
    void entrarComoAdmin() throws Exception {
        tokenAdmin = tokenDe(admin.email(), admin.senha());
    }

    @Test
    void adminCadastraAlunoE201NaoExpoeASenha() throws Exception {
        cadastrar(tokenAdmin, "Bruno", "bruno@email.com", SENHA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("bruno@email.com"))
                .andExpect(jsonPath("$.papel").value("ALUNO"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        Usuario salvo = usuarioRepository.findByEmail("bruno@email.com").orElseThrow();
        assertThat(salvo.getSenha()).isNotEqualTo(SENHA).startsWith("$2");
    }

    @Test
    void cadastroSemTokenDevolve401() throws Exception {
        String corpo = mapeadorJson.writeValueAsString(new RegistroRequest("Ana", "ana@email.com", SENHA));

        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());
        assertThat(usuarioRepository.findByEmail("ana@email.com")).isEmpty();
    }

    @Test
    void alunoNaoPodeCadastrarUsuarios() throws Exception {
        cadastrar(tokenAdmin, "Hugo", "hugo@email.com", SENHA).andExpect(status().isCreated());
        String tokenAluno = tokenDe("hugo@email.com", SENHA);

        cadastrar(tokenAluno, "Amigo do Hugo", "amigo@email.com", SENHA)
                .andExpect(status().isForbidden());
        assertThat(usuarioRepository.findByEmail("amigo@email.com")).isEmpty();
    }

    @Test
    void cadastroIgnoraPapelEnviadoNoCorpo() throws Exception {
        String corpo = """
                {"nome": "Carla", "email": "carla@email.com", "senha": "%s", "papel": "ADMIN"}
                """.formatted(SENHA);

        mockMvc.perform(post("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("ALUNO"));
    }

    @Test
    void cadastroInvalidoDevolve400ComOsCampos() throws Exception {
        cadastrar(tokenAdmin, "", "nao-e-email", "123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo").value(hasItems("nome", "email", "senha")));
    }

    @Test
    void senhaAcentuadaQuePassaDoLimiteDeBytesDoBcryptDevolve400() throws Exception {
        String senhaCom40CaracteresE80Bytes = "é".repeat(40);

        cadastrar(tokenAdmin, "Gil", "gil@email.com", senhaCom40CaracteresE80Bytes)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("senha"));
    }

    @Test
    void cadastroComEmailRepetidoDevolve409() throws Exception {
        cadastrar(tokenAdmin, "Davi", "davi@email.com", SENHA).andExpect(status().isCreated());

        cadastrar(tokenAdmin, "Outro Davi", "DAVI@email.com", SENHA)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(UsuarioService.EMAIL_JA_CADASTRADO));
    }

    @Test
    void loginDevolve200ComTokenQueAbreRotaDeAluno() throws Exception {
        cadastrar(tokenAdmin, "Elisa", "elisa@email.com", SENHA).andExpect(status().isCreated());

        login("elisa@email.com", SENHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value(TokenResponse.TIPO_BEARER));

        mockMvc.perform(post("/inscricoes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenDe("elisa@email.com", SENHA))))
                .andExpect(resultado -> assertThat(resultado.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void loginComSenhaErradaDevolve401() throws Exception {
        cadastrar(tokenAdmin, "Fabio", "fabio@email.com", SENHA).andExpect(status().isCreated());

        login("fabio@email.com", "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(CredenciaisInvalidasException.MENSAGEM));
    }

    @Test
    void loginComEmailDesconhecidoDevolve401() throws Exception {
        login("ninguem@email.com", SENHA)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(CredenciaisInvalidasException.MENSAGEM));
    }

    @Test
    void loginSemCamposDevolve400() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions cadastrar(String token, String nome, String email, String senha) throws Exception {
        String corpo = mapeadorJson.writeValueAsString(new RegistroRequest(nome, email, senha));
        return mockMvc.perform(post("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private ResultActions login(String email, String senha) throws Exception {
        String corpo = mapeadorJson.writeValueAsString(new LoginRequest(email, senha));
        return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private String tokenDe(String email, String senha) throws Exception {
        String resposta = login(email, senha).andReturn().getResponse().getContentAsString();
        return mapeadorJson.readTree(resposta).get("token").asText();
    }

    private static String bearer(String token) {
        return TokenResponse.TIPO_BEARER + " " + token;
    }
}
