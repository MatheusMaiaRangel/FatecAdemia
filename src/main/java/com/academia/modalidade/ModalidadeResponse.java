package com.academia.modalidade;

public record ModalidadeResponse(Long id, String nome, String descricao) {

    public static ModalidadeResponse de(Modalidade modalidade) {
        return new ModalidadeResponse(modalidade.getId(), modalidade.getNome(), modalidade.getDescricao());
    }
}
