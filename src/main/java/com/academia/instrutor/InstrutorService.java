package com.academia.instrutor;

import com.academia.aula.AulaRepository;
import com.academia.erro.RecursoNaoEncontradoException;
import com.academia.erro.RegraNegocioException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstrutorService {

    static final String INSTRUTOR_NAO_ENCONTRADO = "Instrutor não encontrado";
    static final String INSTRUTOR_COM_AULAS = "Não é possível excluir um instrutor que tem aulas cadastradas";

    private final InstrutorRepository instrutorRepository;
    private final AulaRepository aulaRepository;

    public InstrutorService(InstrutorRepository instrutorRepository, AulaRepository aulaRepository) {
        this.instrutorRepository = instrutorRepository;
        this.aulaRepository = aulaRepository;
    }

    @Transactional(readOnly = true)
    public List<InstrutorResponse> listar() {
        return instrutorRepository.findAll(Sort.by("nome", "id")).stream()
                .map(InstrutorResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstrutorResponse buscarPorId(Long id) {
        return InstrutorResponse.de(buscar(id));
    }

    @Transactional
    public InstrutorResponse criar(InstrutorRequest dados) {
        Instrutor instrutor = new Instrutor(dados.nome().trim(), dados.especialidade().trim());
        return InstrutorResponse.de(instrutorRepository.saveAndFlush(instrutor));
    }

    @Transactional
    public InstrutorResponse atualizar(Long id, InstrutorRequest dados) {
        Instrutor instrutor = buscar(id);
        instrutor.atualizar(dados.nome().trim(), dados.especialidade().trim());
        return InstrutorResponse.de(instrutorRepository.saveAndFlush(instrutor));
    }

    @Transactional
    public void excluir(Long id) {
        Instrutor instrutor = buscar(id);
        // Toda aula exige um instrutor: excluir deixaria aulas marcadas e o histórico sem responsável.
        if (aulaRepository.existsByInstrutorId(id)) {
            throw new RegraNegocioException(INSTRUTOR_COM_AULAS);
        }
        instrutorRepository.delete(instrutor);
        instrutorRepository.flush();
    }

    private Instrutor buscar(Long id) {
        return instrutorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(INSTRUTOR_NAO_ENCONTRADO));
    }
}
