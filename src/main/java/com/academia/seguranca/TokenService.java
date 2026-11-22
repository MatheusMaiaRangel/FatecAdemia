package com.academia.seguranca;

import com.academia.usuario.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class TokenService {

    static final String CLAIM_PAPEL = "papel";

    private final SecretKey chave;
    private final Duration expiracao;
    private final String emissor;
    private final Clock relogio;
    private final JwtParser leitor;

    public TokenService(JwtProperties propriedades, Clock relogio) {
        if (propriedades.expiracao().isNegative() || propriedades.expiracao().isZero()) {
            throw new IllegalArgumentException("jwt.expiracao precisa ser maior que zero");
        }
        this.chave = Keys.hmacShaKeyFor(propriedades.segredo().getBytes(StandardCharsets.UTF_8));
        this.expiracao = propriedades.expiracao();
        this.emissor = propriedades.emissor();
        this.relogio = relogio;
        this.leitor = Jwts.parser()
                .verifyWith(chave)
                .requireIssuer(emissor)
                .clock(() -> Date.from(relogio.instant()))
                .build();
    }

    public String gerar(Usuario usuario) {
        Instant agora = relogio.instant();
        return Jwts.builder()
                .issuer(emissor)
                .subject(usuario.getEmail())
                .claim(CLAIM_PAPEL, usuario.getPapel().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracao)))
                .signWith(chave)
                .compact();
    }

    public Optional<String> extrairEmail(String token) {
        try {
            return Optional.ofNullable(leitor.parseSignedClaims(token).getPayload().getSubject());
        } catch (JwtException | IllegalArgumentException tokenInvalido) {
            return Optional.empty();
        }
    }
}
