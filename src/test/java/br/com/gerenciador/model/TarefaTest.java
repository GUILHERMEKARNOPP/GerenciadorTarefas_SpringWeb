package br.com.gerenciador.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TarefaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);
    private final Status pendente = new Status("Pendente", false);
    private final Status concluida = new Status("Concluída", true);

    @Test
    void tarefaVencidaEPendenteEstaAtrasada() {
        assertThat(tarefa(HOJE.minusDays(1), pendente).estaAtrasada(HOJE)).isTrue();
    }

    @Test
    void tarefaQueVenceHojeNaoEstaAtrasada() {
        assertThat(tarefa(HOJE, pendente).estaAtrasada(HOJE)).isFalse();
    }

    @Test
    void tarefaConcluidaOuSemPrazoNuncaEstaAtrasada() {
        assertThat(tarefa(HOJE.minusDays(10), concluida).estaAtrasada(HOJE)).isFalse();
        assertThat(tarefa(null, pendente).estaAtrasada(HOJE)).isFalse();
    }

    @Test
    void concluirTrocaParaOStatusFinalizador() {
        Tarefa tarefa = tarefa(HOJE, pendente);
        tarefa.concluir(concluida);
        assertThat(tarefa.getStatus()).isSameAs(concluida);
        assertThat(tarefa.estaConcluida()).isTrue();
    }

    @Test
    void concluirComStatusQueNaoFinalizaEhErro() {
        assertThatThrownBy(() -> tarefa(HOJE, pendente).concluir(new Status("Em andamento", false)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Tarefa tarefa(LocalDate vencimento, Status status) {
        Tarefa tarefa = new Tarefa(new Usuario("Ana", "ana@example.com", "ana", "hash", Perfil.USUARIO));
        tarefa.setTitulo("Tarefa");
        tarefa.setDataVencimento(vencimento);
        tarefa.setStatus(status);
        return tarefa;
    }
}
