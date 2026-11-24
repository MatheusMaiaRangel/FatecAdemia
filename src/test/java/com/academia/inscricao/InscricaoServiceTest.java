package com.academia.inscricao;

import com.academia.aula.Aula;
import com.academia.aula.AulaRepository;
import com.academia.erro.RecursoNaoEncontradoException;
import com.academia.erro.RegraNegocioException;
import com.academia.instrutor.Instrutor;
import com.academia.modalidade.Modalidade;
import com.academia.seguranca.UsuarioLogado;
import com.academia.usuario.Papel;
import com.academia.usuario.Usuario;
import com.academia.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscricaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-22T12:00:00Z");
    private static final LocalDateTime AGORA_LOCAL = LocalDateTime.ofInstant(AGORA, ZoneOffset.UTC);

    @Mock
    private InscricaoRepository inscricaoRepository;

    @Mock
    private AulaRepository aulaRepository;

    @Mock
    private UsuarioLogado usuarioLogado;

    @Mock
    private UsuarioRepository usuarioRepository;

    private InscricaoService inscricaoService;

    private Usuario aluno;

    @BeforeEach
    void configurar() {
        Clock relogioFixo = Clock.fixed(AGORA, ZoneOffset.UTC);
        inscricaoService = new InscricaoService(inscricaoRepository, aulaRepository, usuarioLogado, usuarioRepository, relogioFixo);

        aluno = new Usuario("Ana", "ana@email.com", "hash", Papel.ALUNO);
        ReflectionTestUtils.setField(aluno, "id", 10L);
    }

    @Test
    void inscreveAlunoQuandoAulaTemVagas() {
        Aula aula = criarAula(1L, AGORA_LOCAL.plusHours(5), 2);
        when(usuarioLogado.obterId()).thenReturn(10L);
        when(usuarioRepository.buscarPorIdParaAtualizar(10L)).thenReturn(Optional.of(aluno));
        when(aulaRepository.buscarPorIdParaAtualizar(1L)).thenReturn(Optional.of(aula));
        when(inscricaoRepository.existsByAlunoIdAndAulaId(10L, 1L)).thenReturn(false);
        when(inscricaoRepository.countByAulaId(1L)).thenReturn(1L);
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(chamada -> {
            Inscricao salva = chamada.getArgument(0);
            ReflectionTestUtils.setField(salva, "id", 99L);
            return salva;
        });

        InscricaoResponse resposta = inscricaoService.inscrever(new InscricaoRequest(1L));

        ArgumentCaptor<Inscricao> captor = ArgumentCaptor.forClass(Inscricao.class);
        verify(inscricaoRepository).save(captor.capture());
        assertThat(captor.getValue().getAluno()).isSameAs(aluno);
        assertThat(captor.getValue().getAula()).isSameAs(aula);
        assertThat(captor.getValue().getCriadaEm()).isEqualTo(AGORA_LOCAL);
        assertThat(resposta.id()).isEqualTo(99L);
        assertThat(resposta.alunoId()).isEqualTo(10L);
        assertThat(resposta.aulaId()).isEqualTo(1L);
        assertThat(resposta.horarioAula()).isEqualTo(aula.getHorario());
    }

    @Test
    void naoPermiteInscreverEmAulaInexistente() {
        when(usuarioLogado.obterId()).thenReturn(10L);
        when(usuarioRepository.buscarPorIdParaAtualizar(10L)).thenReturn(Optional.of(aluno));
        when(aulaRepository.buscarPorIdParaAtualizar(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inscricaoService.inscrever(new InscricaoRequest(404L)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage(InscricaoService.AULA_NAO_ENCONTRADA);
        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    void naoPermiteInscreverDuasVezesNaMesmaAula() {
        Aula aula = criarAula(1L, AGORA_LOCAL.plusHours(5), 10);
        when(usuarioLogado.obterId()).thenReturn(10L);
        when(usuarioRepository.buscarPorIdParaAtualizar(10L)).thenReturn(Optional.of(aluno));
        when(aulaRepository.buscarPorIdParaAtualizar(1L)).thenReturn(Optional.of(aula));
        when(inscricaoRepository.existsByAlunoIdAndAulaId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> inscricaoService.inscrever(new InscricaoRequest(1L)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage(InscricaoService.ALUNO_JA_INSCRITO);
        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    void naoPermiteInscreverEmAulaLotada() {
        Aula aula = criarAula(1L, AGORA_LOCAL.plusHours(5), 2);
        when(usuarioLogado.obterId()).thenReturn(10L);
        when(usuarioRepository.buscarPorIdParaAtualizar(10L)).thenReturn(Optional.of(aluno));
        when(aulaRepository.buscarPorIdParaAtualizar(1L)).thenReturn(Optional.of(aula));
        when(inscricaoRepository.existsByAlunoIdAndAulaId(10L, 1L)).thenReturn(false);
        when(inscricaoRepository.countByAulaId(1L)).thenReturn(2L);

        assertThatThrownBy(() -> inscricaoService.inscrever(new InscricaoRequest(1L)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage(InscricaoService.AULA_LOTADA);
        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    void listaApenasAsInscricoesDoAlunoLogado() {
        Aula aula = criarAula(1L, AGORA_LOCAL.plusHours(4), 10);
        Inscricao inscricao = new Inscricao(aluno, aula, AGORA_LOCAL);
        ReflectionTestUtils.setField(inscricao, "id", 50L);

        when(usuarioLogado.obterId()).thenReturn(10L);
        when(inscricaoRepository.findByAlunoIdOrderByCriadaEmDesc(10L)).thenReturn(List.of(inscricao));

        List<InscricaoResponse> lista = inscricaoService.listarMinhasInscricoes();

        assertThat(lista).hasSize(1);
        assertThat(lista.getFirst().id()).isEqualTo(50L);
        assertThat(lista.getFirst().alunoId()).isEqualTo(10L);
    }

    @Test
    void cancelaInscricaoQuandoFaltamDuasHorasOuMaisParaAAula() {
        Aula aulaExatamenteDuasHoras = criarAula(1L, AGORA_LOCAL.plusHours(2), 10);
        Inscricao inscricao = new Inscricao(aluno, aulaExatamenteDuasHoras, AGORA_LOCAL.minusHours(1));
        ReflectionTestUtils.setField(inscricao, "id", 7L);

        when(usuarioLogado.obterId()).thenReturn(10L);
        when(inscricaoRepository.findByIdAndAlunoId(7L, 10L)).thenReturn(Optional.of(inscricao));

        inscricaoService.cancelar(7L);

        verify(inscricaoRepository).delete(inscricao);
    }

    @Test
    void naoPermiteCancelarComMenosDeDuasHorasParaAAula() {
        Aula aulaEmMenosDeDuasHoras = criarAula(1L, AGORA_LOCAL.plusHours(2).minusMinutes(1), 10);
        Inscricao inscricao = new Inscricao(aluno, aulaEmMenosDeDuasHoras, AGORA_LOCAL.minusHours(1));
        ReflectionTestUtils.setField(inscricao, "id", 7L);

        when(usuarioLogado.obterId()).thenReturn(10L);
        when(inscricaoRepository.findByIdAndAlunoId(7L, 10L)).thenReturn(Optional.of(inscricao));

        assertThatThrownBy(() -> inscricaoService.cancelar(7L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage(InscricaoService.PRAZO_CANCELAMENTO_ENCERRADO);
        verify(inscricaoRepository, never()).delete(any());
    }

    @Test
    void naoPermiteCancelarInscricaoInexistenteOuDeOutroAluno() {
        when(usuarioLogado.obterId()).thenReturn(10L);
        when(inscricaoRepository.findByIdAndAlunoId(999L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inscricaoService.cancelar(999L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage(InscricaoService.INSCRICAO_NAO_ENCONTRADA);
        verify(inscricaoRepository, never()).delete(any());
    }

    @Test
    void rejeitaAulaPassada() {
        verificarAulaIniciada(AGORA_LOCAL.minusDays(2));
    }

    @Test
    void rejeitaAulaNoInstanteAtual() {
        verificarAulaIniciada(AGORA_LOCAL);
    }

    private void verificarAulaIniciada(LocalDateTime horario) {
        when(usuarioLogado.obterId()).thenReturn(10L);
        when(usuarioRepository.buscarPorIdParaAtualizar(10L)).thenReturn(Optional.of(aluno));
        when(aulaRepository.buscarPorIdParaAtualizar(1L)).thenReturn(Optional.of(criarAula(1L, horario, 10)));
        assertThatThrownBy(() -> inscricaoService.inscrever(new InscricaoRequest(1L)))
                .isInstanceOf(RegraNegocioException.class).hasMessage(InscricaoService.AULA_INICIADA);
        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    void rejeitaOutraAulaNoMesmoHorario() {
        LocalDateTime horario = AGORA_LOCAL.plusHours(5);
        when(usuarioLogado.obterId()).thenReturn(10L);
        when(usuarioRepository.buscarPorIdParaAtualizar(10L)).thenReturn(Optional.of(aluno));
        when(aulaRepository.buscarPorIdParaAtualizar(1L)).thenReturn(Optional.of(criarAula(1L, horario, 10)));
        when(inscricaoRepository.existsByAlunoIdAndAulaHorarioAndAulaIdNot(10L, horario, 1L)).thenReturn(true);
        assertThatThrownBy(() -> inscricaoService.inscrever(new InscricaoRequest(1L)))
                .isInstanceOf(RegraNegocioException.class).hasMessage(InscricaoService.CONFLITO_DE_HORARIO);
        verify(inscricaoRepository, never()).save(any());
    }

    private static Aula criarAula(Long id, LocalDateTime horario, int capacidade) {
        Aula aula = new Aula(horario, capacidade,
                new Modalidade("Pilates", "Aula coletiva"), new Instrutor("Carla", "Pilates"));
        ReflectionTestUtils.setField(aula, "id", id);
        return aula;
    }
}
