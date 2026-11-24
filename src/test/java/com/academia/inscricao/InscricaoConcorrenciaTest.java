package com.academia.inscricao;

import com.academia.aula.Aula;
import com.academia.aula.AulaRepository;
import com.academia.aula.AulaRequest;
import com.academia.aula.AulaService;
import com.academia.erro.RegraNegocioException;
import com.academia.instrutor.Instrutor;
import com.academia.instrutor.InstrutorRepository;
import com.academia.modalidade.Modalidade;
import com.academia.modalidade.ModalidadeRepository;
import com.academia.usuario.Papel;
import com.academia.usuario.Usuario;
import com.academia.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:inscricao-concorrencia;DB_CLOSE_DELAY=-1;MODE=MySQL;LOCK_TIMEOUT=15000")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InscricaoConcorrenciaTest {

    @Autowired private InscricaoService service;
    @Autowired private AulaService aulaService;
    @Autowired private AulaRepository aulas;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private InstrutorRepository instrutores;
    @Autowired private ModalidadeRepository modalidades;
    @Autowired private InscricaoRepository inscricoes;
    @Autowired private PlatformTransactionManager transacoes;
    @Autowired private Clock relogio;

    private Modalidade modalidade;
    private Usuario ana;
    private Usuario bruno;
    private LocalDateTime horario;

    @BeforeEach
    void preparar() {
        inscricoes.deleteAll();
        aulas.deleteAll();
        modalidade = modalidades.save(new Modalidade("Pilates", "Aula coletiva"));
        ana = aluno("Ana");
        bruno = aluno("Bruno");
        horario = LocalDateTime.now(relogio).plusDays(5).withNano(0);
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ultimaVagaNaoPodeSerReservadaPorDoisAlunos() throws Exception {
        Aula aula = aula(horario, 1);
        Throwable erro = disputarInscricao(ana, aula, bruno, aula);
        assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage(InscricaoService.AULA_LOTADA);
        assertThat(inscricoes.countByAulaId(aula.getId())).isEqualTo(1);
    }

    @Test
    void mesmoAlunoNaoConsegueReservarAulasSimultaneasEmParalelo() throws Exception {
        Aula primeira = aula(horario, 10);
        Aula segunda = aula(horario, 10);
        Throwable erro = disputarInscricao(ana, primeira, ana, segunda);
        assertThat(erro).isInstanceOf(RegraNegocioException.class).hasMessage(InscricaoService.CONFLITO_DE_HORARIO);
        assertThat(inscricoes.findByAlunoIdOrderByCriadaEmDesc(ana.getId())).hasSize(1);
    }

    @Test
    void mesmoAlunoPodeReservarHorariosDiferentesEmParalelo() throws Exception {
        Throwable erro = disputarInscricao(ana, aula(horario, 10), ana, aula(horario.plusHours(1), 10));
        assertThat(erro).isNull();
        assertThat(inscricoes.findByAlunoIdOrderByCriadaEmDesc(ana.getId())).hasSize(2);
    }

    @Test
    void reducaoDeCapacidadeNaoIgnoraInscricaoEmAndamento() throws Exception {
        Aula aula = aula(horario, 2);
        inscrever(ana, aula);
        Throwable erro = disputarOperacao(bruno, aula, () -> aulaService.atualizar(aula.getId(),
                new AulaRequest(horario, 1, modalidade.getId(), aula.getInstrutor().getId())));
        assertThat(erro).isInstanceOf(RegraNegocioException.class);
        assertThat(inscricoes.countByAulaId(aula.getId())).isEqualTo(2);
        assertThat(aulas.findById(aula.getId()).orElseThrow().getCapacidade()).isEqualTo(2);
    }

    @Test
    void exclusaoNaoIgnoraInscricaoEmAndamento() throws Exception {
        Aula aula = aula(horario, 2);
        Throwable erro = disputarOperacao(ana, aula, () -> {
            aulaService.excluir(aula.getId());
            return true;
        });
        assertThat(erro).isInstanceOf(RegraNegocioException.class);
        assertThat(aulas.findById(aula.getId())).isPresent();
        assertThat(inscricoes.countByAulaId(aula.getId())).isEqualTo(1);
    }

    @Test
    void bloqueiaConflitoTambemAoRemarcarUmaAula() {
        Aula primeira = aula(horario, 10);
        Aula segunda = aula(horario.plusHours(1), 10);
        inscrever(ana, primeira);
        inscrever(ana, segunda);
        assertThatThrownBy(() -> aulaService.atualizar(segunda.getId(),
                new AulaRequest(horario, 10, modalidade.getId(), segunda.getInstrutor().getId())))
                .isInstanceOf(RegraNegocioException.class).hasMessage(InscricaoService.CONFLITO_DE_HORARIO);
        assertThat(aulas.findById(segunda.getId()).orElseThrow().getHorario()).isEqualTo(segunda.getHorario());
    }

    @Test
    void rejeitaAulaPassadaMesmoQueAindaTenhaVagas() {
        Aula passada = aula(LocalDateTime.now(relogio).minusDays(2), 10);
        assertThatThrownBy(() -> inscrever(ana, passada))
                .isInstanceOf(RegraNegocioException.class).hasMessage(InscricaoService.AULA_INICIADA);
        assertThat(inscricoes.countByAulaId(passada.getId())).isZero();
    }

    @Test
    void permiteRemarcarQuandoNaoExisteConflito() {
        Aula aula = aula(horario, 10);
        inscrever(ana, aula);
        assertThat(aulaService.atualizar(aula.getId(), new AulaRequest(horario.plusHours(1),
                10, modalidade.getId(), aula.getInstrutor().getId())).horario()).isEqualTo(horario.plusHours(1));
    }

    // A primeira transação mantém a reserva sem commit. A segunda deve aguardar
    // o bloqueio no banco e validar novamente após a primeira confirmar.
    private Throwable disputarInscricao(Usuario aluno1, Aula aula1, Usuario aluno2, Aula aula2) throws Exception {
        return disputarOperacao(aluno1, aula1, () -> inscrever(aluno2, aula2));
    }

    private Throwable disputarOperacao(Usuario aluno1, Aula aula1, Callable<?> segundaAcao) throws Exception {
        CountDownLatch primeiraGravou = new CountDownLatch(1);
        CountDownLatch liberarCommit = new CountDownLatch(1);
        CountDownLatch segundaIniciou = new CountDownLatch(1);
        TransactionTemplate transacao = new TransactionTemplate(transacoes);
        transacao.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var primeira = pool.submit(() -> transacao.execute(status -> {
                InscricaoResponse resposta = inscrever(aluno1, aula1);
                primeiraGravou.countDown();
                aguardar(liberarCommit);
                return resposta;
            }));
            try {
                assertThat(primeiraGravou.await(10, TimeUnit.SECONDS)).isTrue();
                var segunda = pool.submit(() -> {
                    segundaIniciou.countDown();
                    return segundaAcao.call();
                });
                try {
                    assertThat(segundaIniciou.await(10, TimeUnit.SECONDS)).isTrue();
                    assertThatThrownBy(() -> segunda.get(300, TimeUnit.MILLISECONDS))
                            .isInstanceOf(TimeoutException.class);
                } finally {
                    liberarCommit.countDown();
                }
                assertThat(primeira.get(10, TimeUnit.SECONDS).id()).isNotNull();
                try {
                    assertThat(segunda.get(10, TimeUnit.SECONDS)).isNotNull();
                    return null;
                } catch (ExecutionException erro) {
                    return erro.getCause();
                }
            } finally {
                liberarCommit.countDown();
            }
        }
    }

    private static void aguardar(CountDownLatch sinal) {
        try {
            if (!sinal.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Tempo de espera excedido");
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(erro);
        }
    }

    private InscricaoResponse inscrever(Usuario aluno, Aula aula) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(aluno, null, List.of()));
        try {
            return service.inscrever(new InscricaoRequest(aula.getId()));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Usuario aluno(String nome) {
        return usuarios.save(new Usuario(nome, UUID.randomUUID() + "@teste.local", "hash", Papel.ALUNO));
    }

    private Aula aula(LocalDateTime inicio, int capacidade) {
        Instrutor instrutor = instrutores.save(new Instrutor("Instrutor", "Pilates"));
        return aulas.save(new Aula(inicio, capacidade, modalidade, instrutor));
    }
}
