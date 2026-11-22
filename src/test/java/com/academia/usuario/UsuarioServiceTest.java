package com.academia.usuario;

import com.academia.erro.CredenciaisInvalidasException;
import com.academia.erro.RegraNegocioException;
import com.academia.seguranca.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder codificadorDeSenha;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void registroSempreCriaAlunoComSenhaCriptografadaEEmailNormalizado() {
        when(usuarioRepository.existsByEmail("ana@email.com")).thenReturn(false);
        when(codificadorDeSenha.encode("senha1234")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

        UsuarioResponse resposta = usuarioService.registrarAluno(
                new RegistroRequest("  Ana  ", " Ana@Email.com ", "senha1234"));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertThat(salvo.getValue().getPapel()).isEqualTo(Papel.ALUNO);
        assertThat(salvo.getValue().getSenha()).isEqualTo("hash");
        assertThat(salvo.getValue().getNome()).isEqualTo("Ana");
        assertThat(resposta.email()).isEqualTo("ana@email.com");
        assertThat(resposta.papel()).isEqualTo(Papel.ALUNO);
    }

    @Test
    void registroComEmailJaUsadoViolaRegraDeNegocio() {
        when(usuarioRepository.existsByEmail("ana@email.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrarAluno(
                new RegistroRequest("Ana", "ana@email.com", "senha1234")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage(UsuarioService.EMAIL_JA_CADASTRADO);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void loginComCredenciaisCorretasDevolveToken() {
        Usuario ana = new Usuario("Ana", "ana@email.com", "hash", Papel.ALUNO);
        when(usuarioRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(ana));
        when(codificadorDeSenha.matches("senha1234", "hash")).thenReturn(true);
        when(tokenService.gerar(ana)).thenReturn("token-jwt");

        TokenResponse resposta = usuarioService.autenticar(new LoginRequest("ANA@email.com", "senha1234"));

        assertThat(resposta.token()).isEqualTo("token-jwt");
        assertThat(resposta.tipo()).isEqualTo(TokenResponse.TIPO_BEARER);
    }

    @Test
    void loginComSenhaErradaFalha() {
        Usuario ana = new Usuario("Ana", "ana@email.com", "hash", Papel.ALUNO);
        when(usuarioRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(ana));
        when(codificadorDeSenha.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.autenticar(new LoginRequest("ana@email.com", "errada")))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(tokenService, never()).gerar(any());
    }

    @Test
    void loginComEmailInexistenteFalhaComAMesmaExcecao() {
        when(usuarioRepository.findByEmail("ninguem@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.autenticar(new LoginRequest("ninguem@email.com", "senha1234")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }
}
