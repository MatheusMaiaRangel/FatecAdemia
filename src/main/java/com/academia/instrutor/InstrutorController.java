package com.academia.instrutor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/instrutores")
@Tag(name = "Instrutores", description = "Professores que conduzem as aulas. Alterações são exclusivas do ADMIN.")
public class InstrutorController {

    private final InstrutorService instrutorService;

    public InstrutorController(InstrutorService instrutorService) {
        this.instrutorService = instrutorService;
    }

    @GetMapping
    @Operation(summary = "Lista os instrutores em ordem alfabética", description = "ADMIN e ALUNO podem consultar.")
    public List<InstrutorResponse> listar() {
        return instrutorService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um instrutor pelo ID", description = "Devolve 404 se o instrutor não existir.")
    public InstrutorResponse buscarPorId(@PathVariable @Positive Long id) {
        return instrutorService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um instrutor (ADMIN)",
            description = "Devolve 201 e Location; 400 para dados inválidos.")
    public ResponseEntity<InstrutorResponse> criar(@Valid @RequestBody InstrutorRequest dados) {
        InstrutorResponse resposta = instrutorService.criar(dados);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(resposta.id()).toUri();
        return ResponseEntity.created(local).body(resposta);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza todos os dados de um instrutor (ADMIN)",
            description = "Devolve 400 para dados inválidos e 404 se o instrutor não existir.")
    public InstrutorResponse atualizar(@PathVariable @Positive Long id, @Valid @RequestBody InstrutorRequest dados) {
        return instrutorService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um instrutor sem aulas (ADMIN)",
            description = "Devolve 204; 404 se o instrutor não existir; 409 se ele tiver aulas cadastradas.")
    public void excluir(@PathVariable @Positive Long id) {
        instrutorService.excluir(id);
    }
}
