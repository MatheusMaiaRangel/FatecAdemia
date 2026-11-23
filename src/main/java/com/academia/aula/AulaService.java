package com.academia.aula;

import com.academia.erro.RecursoNaoEncontradoException;
import com.academia.erro.RegraNegocioException;
import com.academia.inscricao.InscricaoRepository;
import com.academia.instrutor.Instrutor;
import com.academia.instrutor.InstrutorRepository;
import com.academia.modalidade.Modalidade;
import com.academia.modalidade.ModalidadeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Service
public class AulaService {

    static final String AULA_NAO_ENCONTRADA = "Aula não encontrada";
    static final String MODALIDADE_NAO_ENCONTRADA = "Modalidade não encontrada";
    static final String INSTRUTOR_NAO_ENCONTRADO = "Instrutor não encontrado";
    static final String CONFLITO_DE_HORARIO = "O instrutor já tem uma aula neste horário";
    static final String CAPACIDADE_INSUFICIENTE = "A capacidade não pode ser menor que o número de inscritos";
    static final String AULA_COM_INSCRICOES = "Não é possível excluir uma aula com inscrições";

    private final AulaRepository aulaRepository;
    private final ModalidadeRepository modalidadeRepository;
    private final InstrutorRepository instrutorRepository;
    private final InscricaoRepository inscricaoRepository;
    private final Clock relogio;

    public AulaService(AulaRepository aulaRepository, ModalidadeRepository modalidadeRepository,
                       InstrutorRepository instrutorRepository, InscricaoRepository inscricaoRepository,
                       Clock relogio) {
        this.aulaRepository = aulaRepository;
        this.modalidadeRepository = modalidadeRepository;
        this.instrutorRepository = instrutorRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Page<AulaResponse> listar(String modalidade, Pageable pageable) {
        Page<Aula> aulas = modalidade == null
                ? aulaRepository.findAll(pageable)
                : aulaRepository.findByModalidadeNomeContainingIgnoreCase(modalidade.trim(), pageable);
        return aulas.map(AulaResponse::de);
    }

    @Transactional(readOnly = true)
    public Page<AulaResponse> listarProximas(int pagina, int tamanho) {
        // A ordem por horário pertence à consulta; o ID desempata aulas simultâneas.
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("id"));
        return aulaRepository.findByHorarioAfterOrderByHorarioAsc(LocalDateTime.now(relogio), pageable)
                .map(AulaResponse::de);
    }

    @Transactional(readOnly = true)
    public AulaResponse buscarPorId(Long id) {
        return AulaResponse.de(buscarAula(id));
    }

    @Transactional
    public AulaResponse criar(AulaRequest dados) {
        Modalidade modalidade = buscarModalidade(dados.modalidadeId());
        Instrutor instrutor = buscarInstrutor(dados.instrutorId());
        validarHorario(instrutor.getId(), dados.horario(), null);
        Aula aula = new Aula(dados.horario(), dados.capacidade(), modalidade, instrutor);
        return AulaResponse.de(aulaRepository.saveAndFlush(aula));
    }

    @Transactional
    public AulaResponse atualizar(Long id, AulaRequest dados) {
        Aula aula = buscarAula(id);
        Modalidade modalidade = buscarModalidade(dados.modalidadeId());
        Instrutor instrutor = buscarInstrutor(dados.instrutorId());
        validarHorario(instrutor.getId(), dados.horario(), id);
        if (dados.capacidade() < inscricaoRepository.countByAulaId(id)) {
            throw new RegraNegocioException(CAPACIDADE_INSUFICIENTE);
        }
        aula.atualizar(dados.horario(), dados.capacidade(), modalidade, instrutor);
        return AulaResponse.de(aulaRepository.saveAndFlush(aula));
    }

    @Transactional
    public void excluir(Long id) {
        Aula aula = buscarAula(id);
        if (inscricaoRepository.countByAulaId(id) > 0) {
            throw new RegraNegocioException(AULA_COM_INSCRICOES);
        }
        aulaRepository.delete(aula);
        aulaRepository.flush();
    }

    private void validarHorario(Long instrutorId, LocalDateTime horario, Long aulaId) {
        LocalDateTime inicioDoDia = horario.toLocalDate().atStartOfDay();
        LocalDateTime fimDoDia = horario.toLocalDate().atTime(LocalTime.MAX);
        // O modelo tem apenas o início da aula: conflito significa o mesmo instante de início.
        boolean conflito = aulaRepository.findByInstrutorIdAndHorarioBetween(instrutorId, inicioDoDia, fimDoDia)
                .stream()
                .anyMatch(outra -> !Objects.equals(outra.getId(), aulaId) && outra.getHorario().isEqual(horario));
        if (conflito) {
            throw new RegraNegocioException(CONFLITO_DE_HORARIO);
        }
    }

    private Aula buscarAula(Long id) {
        return aulaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(AULA_NAO_ENCONTRADA));
    }

    private Modalidade buscarModalidade(Long id) {
        return modalidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(MODALIDADE_NAO_ENCONTRADA));
    }

    private Instrutor buscarInstrutor(Long id) {
        return instrutorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(INSTRUTOR_NAO_ENCONTRADO));
    }
}
