package com.academia.seguranca;

import com.academia.usuario.Papel;
import com.academia.usuario.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    static final String[] ROTAS_PUBLICAS = {
            "/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/error"
    };
    static final String[] ROTAS_DO_CATALOGO = {"/modalidades/**", "/instrutores/**", "/aulas/**"};
    static final String ROTAS_DE_INSCRICAO = "/inscricoes/**";

    @Bean
    public SecurityFilterChain cadeiaDeFiltros(HttpSecurity http, TokenService tokenService,
                                               UsuarioRepository usuarioRepository,
                                               RespostaErroSeguranca respostaErro) throws Exception {
        // Sem sessão nem cookie de login, o token vai no cabeçalho; por isso o CSRF não se aplica.
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.GET, ROTAS_DO_CATALOGO).authenticated()
                        .requestMatchers(ROTAS_DO_CATALOGO).hasRole(Papel.ADMIN.name())
                        .requestMatchers(ROTAS_DE_INSCRICAO).hasRole(Papel.ALUNO.name())
                        .anyRequest().authenticated())
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(respostaErro)
                        .accessDeniedHandler(respostaErro))
                .addFilterBefore(new FiltroJwt(tokenService, usuarioRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }
}
