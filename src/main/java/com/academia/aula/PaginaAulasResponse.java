package com.academia.aula;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaAulasResponse(List<AulaResponse> conteudo, int pagina, int tamanho,
                                 long totalElementos, int totalPaginas) {

    public static PaginaAulasResponse de(Page<AulaResponse> pagina) {
        return new PaginaAulasResponse(pagina.getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
