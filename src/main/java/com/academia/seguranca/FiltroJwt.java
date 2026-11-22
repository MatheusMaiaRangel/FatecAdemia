package com.academia.seguranca;

import com.academia.usuario.Usuario;
import com.academia.usuario.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class FiltroJwt extends OncePerRequestFilter {

    static final String PREFIXO_BEARER = "Bearer ";
    static final String PREFIXO_PAPEL = "ROLE_";

    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;

    public FiltroJwt(TokenService tokenService, UsuarioRepository usuarioRepository) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {
        extrairToken(requisicao)
                .flatMap(tokenService::extrairEmail)
                .flatMap(usuarioRepository::findByEmail)
                .ifPresent(this::autenticar);
        cadeia.doFilter(requisicao, resposta);
    }

    private Optional<String> extrairToken(HttpServletRequest requisicao) {
        String cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO_BEARER)) {
            return Optional.empty();
        }
        return Optional.of(cabecalho.substring(PREFIXO_BEARER.length()).trim());
    }

    private void autenticar(Usuario usuario) {
        var autoridades = List.of(new SimpleGrantedAuthority(PREFIXO_PAPEL + usuario.getPapel().name()));
        var autenticacao = new UsernamePasswordAuthenticationToken(usuario, null, autoridades);
        SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }
}
