package com.academia.inscricao;

import com.academia.aula.Aula;
import com.academia.aula.AulaRepository;
import com.academia.erro.RecursoNaoEncontradoException;
import com.academia.erro.RegraNegocioException;
import com.academia.seguranca.UsuarioLogado;
import com.academia.usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InscricaoService {

    static final Duration ANTECEDENCIA_MINIMA_CANCELAMENTO = Duration.ofHours(2);
    static final String AULA_NAO_ENCONTRADA = "Aula não encontrada";
    static final String INSCRICAO_NAO_ENCONTRADA = "Inscrição não encontrada";
    static final String AULA_LOTADA = "Aula lotada";
    static final String ALUNO_JA_INSCRITO = "Você já está inscrito nesta aula";
    static final String PRAZO_CANCELAMENTO_ENCERRADO =
            "Não é possível cancelar a inscrição com menos de 2 horas para a aula";

    private final InscricaoRepository inscricaoRepository;
    private final AulaRepository aulaRepository;
    private final UsuarioLogado usuarioLogado;
    private final Clock relogio;

    public InscricaoService(InscricaoRepository inscricaoRepository,
                            AulaRepository aulaRepository,
                            UsuarioLogado usuarioLogado,
                            Clock relogio) {
        this.inscricaoRepository = inscricaoRepository;
        this.aulaRepository = aulaRepository;
        this.usuarioLogado = usuarioLogado;
        this.relogio = relogio;
    }

    @Transactional
    public InscricaoResponse inscrever(InscricaoRequest dados) {
        Usuario aluno = usuarioLogado.obter();
        Aula aula = aulaRepository.findById(dados.aulaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(AULA_NAO_ENCONTRADA));

        if (inscricaoRepository.existsByAlunoIdAndAulaId(aluno.getId(), aula.getId())) {
            throw new RegraNegocioException(ALUNO_JA_INSCRITO);
        }

        long totalInscritos = inscricaoRepository.countByAulaId(aula.getId());
        if (totalInscritos >= aula.getCapacidade()) {
            throw new RegraNegocioException(AULA_LOTADA);
        }

        Inscricao inscricao = new Inscricao(aluno, aula, LocalDateTime.now(relogio));
        return InscricaoResponse.de(inscricaoRepository.save(inscricao));
    }

    @Transactional(readOnly = true)
    public List<InscricaoResponse> listarMinhasInscricoes() {
        Long alunoId = usuarioLogado.obterId();
        return inscricaoRepository.findByAlunoIdOrderByCriadaEmDesc(alunoId).stream()
                .map(InscricaoResponse::de)
                .toList();
    }

    @Transactional
    public void cancelar(Long id) {
        Long alunoId = usuarioLogado.obterId();
        Inscricao inscricao = inscricaoRepository.findByIdAndAlunoId(id, alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(INSCRICAO_NAO_ENCONTRADA));

        LocalDateTime agora = LocalDateTime.now(relogio);
        LocalDateTime limiteCancelamento = inscricao.getAula().getHorario().minus(ANTECEDENCIA_MINIMA_CANCELAMENTO);
        if (agora.isAfter(limiteCancelamento)) {
            throw new RegraNegocioException(PRAZO_CANCELAMENTO_ENCERRADO);
        }

        inscricaoRepository.delete(inscricao);
    }
}
