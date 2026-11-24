package com.academia.erro;

import com.academia.aula.Aula;
import com.academia.aula.AulaRepository;
import com.academia.inscricao.Inscricao;
import com.academia.inscricao.InscricaoRepository;
import com.academia.instrutor.Instrutor;
import com.academia.instrutor.InstrutorRepository;
import com.academia.modalidade.Modalidade;
import com.academia.modalidade.ModalidadeRepository;
import com.academia.usuario.Papel;
import com.academia.usuario.Usuario;
import com.academia.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

// Confere, no banco de verdade, que cada restrição única chega ao tratador com um nome reconhecível.
@SpringBootTest
@ActiveProfiles("test")
class RestricoesDoBancoTest {

    private static final LocalDateTime HORARIO = LocalDateTime.of(2030, 1, 10, 18, 0);

    @Autowired private UsuarioRepository usuarios;
    @Autowired private ModalidadeRepository modalidades;
    @Autowired private InstrutorRepository instrutores;
    @Autowired private AulaRepository aulas;
    @Autowired private InscricaoRepository inscricoes;

    @AfterEach
    void limpar() {
        inscricoes.deleteAll();
        aulas.deleteAll();
        instrutores.deleteAll();
        modalidades.deleteAll();
        usuarios.findAll().stream().filter(u -> u.getPapel() == Papel.ALUNO).forEach(usuarios::delete);
    }

    @Test
    void emailRepetidoViraMensagemDeEmail() {
        usuarios.saveAndFlush(aluno("repetido@teste.local"));

        var erro = catchThrowableOfType(DataIntegrityViolationException.class,
                () -> usuarios.saveAndFlush(aluno("repetido@teste.local")));

        assertThat(TratadorDeErros.mensagemDaRestricao(erro)).isEqualTo("Já existe um usuário com este e-mail");
    }

    @Test
    void instrutorNoMesmoHorarioViraMensagemDeConflito() {
        Aula aula = novaAula();

        var erro = catchThrowableOfType(DataIntegrityViolationException.class,
                () -> aulas.saveAndFlush(new Aula(HORARIO, 5, aula.getModalidade(), aula.getInstrutor())));

        assertThat(TratadorDeErros.mensagemDaRestricao(erro)).isEqualTo("O instrutor já tem uma aula neste horário");
    }

    @Test
    void inscricaoRepetidaViraMensagemDeJaInscrito() {
        Usuario aluno = usuarios.saveAndFlush(aluno("ana@teste.local"));
        Aula aula = novaAula();
        inscricoes.saveAndFlush(new Inscricao(aluno, aula, HORARIO.minusDays(1)));

        var erro = catchThrowableOfType(DataIntegrityViolationException.class,
                () -> inscricoes.saveAndFlush(new Inscricao(aluno, aula, HORARIO.minusDays(1))));

        assertThat(TratadorDeErros.mensagemDaRestricao(erro)).isEqualTo("Você já está inscrito nesta aula");
    }

    private Aula novaAula() {
        Modalidade modalidade = modalidades.saveAndFlush(new Modalidade("Pilates", "Aula coletiva"));
        Instrutor instrutor = instrutores.saveAndFlush(new Instrutor("Carla", "Pilates"));
        return aulas.saveAndFlush(new Aula(HORARIO, 5, modalidade, instrutor));
    }

    private static Usuario aluno(String email) {
        return new Usuario("Aluno", email, "hash", Papel.ALUNO);
    }
}
