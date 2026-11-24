package com.academia.modalidade;

import com.academia.aula.AulaRepository;
import com.academia.erro.RecursoNaoEncontradoException;
import com.academia.erro.RegraNegocioException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModalidadeService {

    static final String MODALIDADE_NAO_ENCONTRADA = "Modalidade não encontrada";
    static final String MODALIDADE_COM_AULAS_FUTURAS =
            "Não é possível excluir uma modalidade que ainda tem aulas marcadas";
    static final String MODALIDADE_COM_HISTORICO =
            "Não é possível excluir uma modalidade com aulas já realizadas no histórico";

    private final ModalidadeRepository modalidadeRepository;
    private final AulaRepository aulaRepository;
    private final Clock relogio;

    public ModalidadeService(ModalidadeRepository modalidadeRepository, AulaRepository aulaRepository,
                             Clock relogio) {
        this.modalidadeRepository = modalidadeRepository;
        this.aulaRepository = aulaRepository;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<ModalidadeResponse> listar() {
        return modalidadeRepository.findAll(Sort.by("nome", "id")).stream()
                .map(ModalidadeResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ModalidadeResponse buscarPorId(Long id) {
        return ModalidadeResponse.de(buscar(id));
    }

    @Transactional
    public ModalidadeResponse criar(ModalidadeRequest dados) {
        Modalidade modalidade = new Modalidade(dados.nome().trim(), descricaoLimpa(dados.descricao()));
        return ModalidadeResponse.de(modalidadeRepository.saveAndFlush(modalidade));
    }

    @Transactional
    public ModalidadeResponse atualizar(Long id, ModalidadeRequest dados) {
        Modalidade modalidade = buscar(id);
        modalidade.atualizar(dados.nome().trim(), descricaoLimpa(dados.descricao()));
        return ModalidadeResponse.de(modalidadeRepository.saveAndFlush(modalidade));
    }

    @Transactional
    public void excluir(Long id) {
        Modalidade modalidade = buscar(id);
        // Regra 4: aula marcada perderia a modalidade; aula passada guarda o histórico das inscrições.
        if (aulaRepository.existsByModalidadeIdAndHorarioAfter(id, LocalDateTime.now(relogio))) {
            throw new RegraNegocioException(MODALIDADE_COM_AULAS_FUTURAS);
        }
        if (aulaRepository.existsByModalidadeId(id)) {
            throw new RegraNegocioException(MODALIDADE_COM_HISTORICO);
        }
        modalidadeRepository.delete(modalidade);
        modalidadeRepository.flush();
    }

    private Modalidade buscar(Long id) {
        return modalidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(MODALIDADE_NAO_ENCONTRADA));
    }

    private static String descricaoLimpa(String descricao) {
        return descricao == null || descricao.isBlank() ? null : descricao.trim();
    }
}
