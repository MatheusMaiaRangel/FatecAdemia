package com.academia.inscricao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inscricoes")
@Tag(name = "Inscrições", description = "Inscrições do aluno logado em aulas coletivas")
public class InscricaoController {

    private final InscricaoService inscricaoService;

    public InscricaoController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Inscreve o aluno logado em uma aula",
            description = "Devolve 404 se a aula não existir e 409 se a aula estiver lotada ou o aluno já estiver inscrito.")
    public InscricaoResponse inscrever(@Valid @RequestBody InscricaoRequest dados) {
        return inscricaoService.inscrever(dados);
    }

    @GetMapping
    @Operation(summary = "Lista as inscrições do aluno logado",
            description = "Devolve apenas as inscrições pertencentes ao aluno autenticado.")
    public List<InscricaoResponse> listarMinhasInscricoes() {
        return inscricaoService.listarMinhasInscricoes();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela uma inscrição do aluno logado",
            description = "Devolve 404 se a inscrição não existir ou pertencer a outro aluno, e 409 se faltar menos de 2 horas para a aula.")
    public void cancelar(@PathVariable Long id) {
        inscricaoService.cancelar(id);
    }
}
