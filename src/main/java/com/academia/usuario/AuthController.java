package com.academia.usuario;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Cadastro de alunos e login")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo aluno",
            description = "Todo cadastro público recebe o papel ALUNO. Devolve 409 se o e-mail já existir.")
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest dados) {
        return usuarioService.registrarAluno(dados);
    }

    @PostMapping("/login")
    @Operation(summary = "Faz login e devolve o token JWT",
            description = "Use o token no botão Authorize ou no cabeçalho Authorization: Bearer <token>.")
    public TokenResponse login(@Valid @RequestBody LoginRequest dados) {
        return usuarioService.autenticar(dados);
    }
}
