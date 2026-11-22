package com.academia.seguranca;

import com.academia.usuario.Papel;
import com.academia.usuario.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioLogadoTest {

    private final UsuarioLogado usuarioLogado = new UsuarioLogado();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devolveOUsuarioAutenticadoPeloFiltro() {
        Usuario ana = new Usuario("Ana", "ana@email.com", "hash", Papel.ALUNO);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(ana, null, List.of()));

        assertThat(usuarioLogado.obter()).isSameAs(ana);
        assertThat(usuarioLogado.obterId()).isEqualTo(ana.getId());
    }

    @Test
    void semAutenticacaoLancaErro() {
        assertThatThrownBy(usuarioLogado::obter)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(UsuarioLogado.SEM_USUARIO_LOGADO);
    }

    @Test
    void autenticacaoSemUsuarioDoSistemaLancaErro() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("anonimo", null, List.of()));

        assertThatThrownBy(usuarioLogado::obter).isInstanceOf(IllegalStateException.class);
    }
}
