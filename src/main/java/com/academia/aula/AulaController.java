package com.academia.aula;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/aulas")
@Tag(name = "Aulas", description = "Catálogo de aulas coletivas. Alterações são exclusivas do ADMIN.")
public class AulaController {

    private static final Set<String> CAMPOS_ORDENAVEIS =
            Set.of("id", "horario", "capacidade", "modalidade.nome", "instrutor.nome");

    private final AulaService aulaService;

    public AulaController(AulaService aulaService) {
        this.aulaService = aulaService;
    }

    @GetMapping
    @Operation(summary = "Lista aulas com paginação, ordenação e filtro por modalidade",
            description = "ADMIN e ALUNO podem consultar. O filtro busca parte do nome, sem diferenciar maiúsculas.")
    public PaginaAulasResponse listar(
            @RequestParam(required = false) @Size(min = 1, max = 100) @Pattern(regexp = ".*\\S.*")
            String modalidade,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "Campo,direção: id, horario, capacidade, modalidade.nome ou instrutor.nome; asc ou desc",
                    schema = @Schema(defaultValue = "horario,asc"))
            @RequestParam(required = false) String sort) {
        String ordenacao = sort == null ? "horario,asc" : sort;
        return PaginaAulasResponse.de(aulaService.listar(modalidade, paginacao(page, size, ordenacao)));
    }

    @GetMapping("/proximas")
    @Operation(summary = "Lista somente aulas futuras, da mais próxima para a mais distante",
            description = "Usa o relógio da academia e paginação. ADMIN e ALUNO podem consultar.")
    public PaginaAulasResponse listarProximas(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return PaginaAulasResponse.de(aulaService.listarProximas(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma aula pelo ID", description = "Devolve 404 se a aula não existir.")
    public AulaResponse buscarPorId(@PathVariable @Positive Long id) {
        return aulaService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cria uma aula (ADMIN)",
            description = "Devolve 201 e Location; 400 para dados inválidos; 404 para modalidade/instrutor inexistente; "
                    + "409 se o instrutor já tiver aula no mesmo horário de início.")
    public ResponseEntity<AulaResponse> criar(@Valid @RequestBody AulaRequest dados) {
        AulaResponse resposta = aulaService.criar(dados);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(resposta.id()).toUri();
        return ResponseEntity.created(local).body(resposta);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza todos os dados de uma aula (ADMIN)",
            description = "Devolve 404 para IDs inexistentes e 409 para conflito do instrutor ou capacidade inferior "
                    + "ao número de inscritos. A própria aula não causa conflito de horário.")
    public AulaResponse atualizar(@PathVariable @Positive Long id, @Valid @RequestBody AulaRequest dados) {
        return aulaService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma aula sem inscrições (ADMIN)",
            description = "Devolve 204; 404 se a aula não existir; 409 se houver inscrições vinculadas.")
    public void excluir(@PathVariable @Positive Long id) {
        aulaService.excluir(id);
    }

    private static Pageable paginacao(int page, int size, String sort) {
        String[] partes = sort.split(",", -1);
        if (partes.length != 2 || !CAMPOS_ORDENAVEIS.contains(partes[0])
                || !(partes[1].equalsIgnoreCase("asc") || partes[1].equalsIgnoreCase("desc"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ordenação inválida. Use campo,asc ou campo,desc para um campo permitido.");
        }
        Sort ordem = Sort.by(Sort.Direction.fromString(partes[1]), partes[0]);
        if (!partes[0].equals("id")) {
            ordem = ordem.and(Sort.by("id"));
        }
        return PageRequest.of(page, size, ordem);
    }
}
