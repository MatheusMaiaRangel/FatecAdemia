package com.academia.usuario;

import com.academia.erro.CredenciaisInvalidasException;
import com.academia.erro.RegraNegocioException;
import com.academia.seguranca.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UsuarioService {

    static final String EMAIL_JA_CADASTRADO = "Já existe um usuário com este e-mail";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder codificadorDeSenha;
    private final TokenService tokenService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder codificadorDeSenha,
                          TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.codificadorDeSenha = codificadorDeSenha;
        this.tokenService = tokenService;
    }

    @Transactional
    public UsuarioResponse registrarAluno(RegistroRequest dados) {
        String email = normalizarEmail(dados.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraNegocioException(EMAIL_JA_CADASTRADO);
        }
        Usuario aluno = new Usuario(dados.nome().trim(), email,
                codificadorDeSenha.encode(dados.senha()), Papel.ALUNO);
        return UsuarioResponse.de(usuarioRepository.save(aluno));
    }

    @Transactional
    public boolean criarAdminSeNaoExistir(AdminProperties admin) {
        String email = normalizarEmail(admin.email());
        if (usuarioRepository.existsByEmail(email)) {
            return false;
        }
        usuarioRepository.save(new Usuario(admin.nome().trim(), email,
                codificadorDeSenha.encode(admin.senha()), Papel.ADMIN));
        return true;
    }

    @Transactional(readOnly = true)
    public TokenResponse autenticar(LoginRequest dados) {
        Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(dados.email()))
                .filter(encontrado -> codificadorDeSenha.matches(dados.senha(), encontrado.getSenha()))
                .orElseThrow(CredenciaisInvalidasException::new);
        return TokenResponse.bearer(tokenService.gerar(usuario));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
