package com.academia.modalidade;

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
@RequestMapping("/modalidades")
@Tag(name = "Modalidades", description = "Tipos de aula oferecidos pela academia. Alterações são exclusivas do ADMIN.")
public class ModalidadeController {

    private final ModalidadeService modalidadeService;

    public ModalidadeController(ModalidadeService modalidadeService) {
        this.modalidadeService = modalidadeService;
    }

    @GetMapping
    @Operation(summary = "Lista as modalidades em ordem alfabética", description = "ADMIN e ALUNO podem consultar.")
    public List<ModalidadeResponse> listar() {
        return modalidadeService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma modalidade pelo ID", description = "Devolve 404 se a modalidade não existir.")
    public ModalidadeResponse buscarPorId(@PathVariable @Positive Long id) {
        return modalidadeService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cria uma modalidade (ADMIN)",
            description = "Devolve 201 e Location; 400 para dados inválidos.")
    public ResponseEntity<ModalidadeResponse> criar(@Valid @RequestBody ModalidadeRequest dados) {
        ModalidadeResponse resposta = modalidadeService.criar(dados);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(resposta.id()).toUri();
        return ResponseEntity.created(local).body(resposta);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza todos os dados de uma modalidade (ADMIN)",
            description = "Devolve 400 para dados inválidos e 404 se a modalidade não existir.")
    public ModalidadeResponse atualizar(@PathVariable @Positive Long id, @Valid @RequestBody ModalidadeRequest dados) {
        return modalidadeService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma modalidade sem aulas (ADMIN)",
            description = "Devolve 204; 404 se a modalidade não existir; 409 se ela ainda tiver aulas marcadas "
                    + "ou aulas já realizadas no histórico.")
    public void excluir(@PathVariable @Positive Long id) {
        modalidadeService.excluir(id);
    }
}
