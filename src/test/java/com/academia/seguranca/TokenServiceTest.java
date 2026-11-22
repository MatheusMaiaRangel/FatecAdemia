package com.academia.seguranca;

import com.academia.usuario.Papel;
import com.academia.usuario.Usuario;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes-de-tamanho";
    private static final String OUTRO_SEGREDO = "outro-segredo-de-teste-com-mais-de-32-bytes";
    private static final String EMISSOR = "FatecAdemia";
    private static final Duration EXPIRACAO = Duration.ofHours(2);
    private static final Instant AGORA = Instant.parse("2026-01-10T10:00:00Z");

    private final Usuario aluno = new Usuario("Ana", "ana@email.com", "hash", Papel.ALUNO);

    @Test
    void geraTokenQueDevolveOEmailDoUsuario() {
        TokenService servico = criar(SEGREDO, relogioEm(AGORA));

        String token = servico.gerar(aluno);

        assertThat(servico.extrairEmail(token)).contains("ana@email.com");
    }

    @Test
    void tokenExpiradoNaoEhAceito() {
        String token = criar(SEGREDO, relogioEm(AGORA)).gerar(aluno);
        TokenService depoisDaExpiracao = criar(SEGREDO, relogioEm(AGORA.plus(EXPIRACAO).plusSeconds(1)));

        assertThat(depoisDaExpiracao.extrairEmail(token)).isEmpty();
    }

    @Test
    void tokenAssinadoComOutraChaveNaoEhAceito() {
        String token = criar(OUTRO_SEGREDO, relogioEm(AGORA)).gerar(aluno);

        assertThat(criar(SEGREDO, relogioEm(AGORA)).extrairEmail(token)).isEmpty();
    }

    @Test
    void tokenDeOutroEmissorNaoEhAceito() {
        JwtProperties outroEmissor = new JwtProperties(SEGREDO, EXPIRACAO, "OutroSistema");
        String token = new TokenService(outroEmissor, relogioEm(AGORA)).gerar(aluno);

        assertThat(criar(SEGREDO, relogioEm(AGORA)).extrairEmail(token)).isEmpty();
    }

    @Test
    void textoQueNaoEhTokenNaoEhAceito() {
        TokenService servico = criar(SEGREDO, relogioEm(AGORA));

        assertThat(servico.extrairEmail("nao-e-um-token")).isEmpty();
        assertThat(servico.extrairEmail("")).isEmpty();
    }

    @Test
    void segredoCurtoImpedeASubidaDoServico() {
        assertThatThrownBy(() -> criar("curto", relogioEm(AGORA)))
                .isInstanceOf(WeakKeyException.class);
    }

    @Test
    void expiracaoNaoPositivaImpedeASubidaDoServico() {
        JwtProperties semExpiracao = new JwtProperties(SEGREDO, Duration.ZERO, EMISSOR);

        assertThatThrownBy(() -> new TokenService(semExpiracao, relogioEm(AGORA)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static TokenService criar(String segredo, Clock relogio) {
        return new TokenService(new JwtProperties(segredo, EXPIRACAO, EMISSOR), relogio);
    }

    private static Clock relogioEm(Instant instante) {
        return Clock.fixed(instante, ZoneOffset.UTC);
    }
}
