package com.academia.inscricao;

import com.academia.aula.Aula;
import com.academia.aula.AulaRepository;
import com.academia.erro.RecursoNaoEncontradoException;
import com.academia.erro.RegraNegocioException;
import com.academia.seguranca.UsuarioLogado;
import com.academia.usuario.Usuario;
import com.academia.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
    static final String AULA_INICIADA = "Não é possível se inscrever em uma aula que já começou";
    public static final String CONFLITO_DE_HORARIO = "O aluno já está inscrito em outra aula neste horário";
    static final String PRAZO_CANCELAMENTO_ENCERRADO =
            "Não é possível cancelar a inscrição com menos de 2 horas para a aula";

    private final InscricaoRepository inscricaoRepository;
    private final AulaRepository aulaRepository;
    private final UsuarioLogado usuarioLogado;
    private final UsuarioRepository usuarioRepository;
    private final Clock relogio;

    public InscricaoService(InscricaoRepository inscricaoRepository,
                            AulaRepository aulaRepository,
                            UsuarioLogado usuarioLogado,
                            UsuarioRepository usuarioRepository,
                            Clock relogio) {
        this.inscricaoRepository = inscricaoRepository;
        this.aulaRepository = aulaRepository;
        this.usuarioLogado = usuarioLogado;
        this.usuarioRepository = usuarioRepository;
        this.relogio = relogio;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InscricaoResponse inscrever(InscricaoRequest dados) {
        // Sempre aluno antes da aula: serializa tanto a agenda quanto a última vaga.
        Usuario aluno = usuarioRepository.buscarPorIdParaAtualizar(usuarioLogado.obterId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado"));
        Aula aula = aulaRepository.buscarPorIdParaAtualizar(dados.aulaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(AULA_NAO_ENCONTRADA));

        LocalDateTime agora = LocalDateTime.now(relogio);
        if (!aula.getHorario().isAfter(agora)) {
            throw new RegraNegocioException(AULA_INICIADA);
        }

        if (inscricaoRepository.existsByAlunoIdAndAulaId(aluno.getId(), aula.getId())) {
            throw new RegraNegocioException(ALUNO_JA_INSCRITO);
        }

        if (inscricaoRepository.existsByAlunoIdAndAulaHorarioAndAulaIdNot(
                aluno.getId(), aula.getHorario(), aula.getId())) {
            throw new RegraNegocioException(CONFLITO_DE_HORARIO);
        }

        long totalInscritos = inscricaoRepository.countByAulaId(aula.getId());
        if (totalInscritos >= aula.getCapacidade()) {
            throw new RegraNegocioException(AULA_LOTADA);
        }

        Inscricao inscricao = new Inscricao(aluno, aula, agora);
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
