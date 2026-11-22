package com.academia.usuario;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminInicialTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapeadorJson;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AdminProperties admin;

    @Autowired
    private AdminInicial adminInicial;

    @Test
    void adminDasVariaveisDeAmbienteEhCriadoNaSubidaEConsegueAlterarOCatalogo() throws Exception {
        Usuario salvo = usuarioRepository.findByEmail(admin.email()).orElseThrow();
        assertThat(salvo.getPapel()).isEqualTo(Papel.ADMIN);

        String corpo = mapeadorJson.writeValueAsString(new LoginRequest(admin.email(), admin.senha()));
        String resposta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = mapeadorJson.readTree(resposta).get("token").asText();

        mockMvc.perform(post("/modalidades").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(resultado -> assertThat(resultado.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void rodarDeNovoNaoDuplicaOAdmin() {
        long antes = usuarioRepository.count();

        adminInicial.run(null);

        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }

    @Test
    void senhaNaoApareceAoImprimirAsPropriedades() {
        assertThat(admin.toString()).doesNotContain(admin.senha());
    }
}
