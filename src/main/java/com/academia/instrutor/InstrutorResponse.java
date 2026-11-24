package com.academia.instrutor;

public record InstrutorResponse(Long id, String nome, String especialidade) {

    public static InstrutorResponse de(Instrutor instrutor) {
        return new InstrutorResponse(instrutor.getId(), instrutor.getNome(), instrutor.getEspecialidade());
    }
}
