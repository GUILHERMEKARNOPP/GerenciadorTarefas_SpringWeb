package br.com.gerenciador.service;

import br.com.gerenciador.dto.StatusForm;
import br.com.gerenciador.model.Status;
import br.com.gerenciador.repository.StatusRepository;
import br.com.gerenciador.repository.TarefaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

    @Mock
    private StatusRepository statusRepository;
    @Mock
    private TarefaRepository tarefaRepository;
    @InjectMocks
    private StatusService statusService;

    @Test
    void naoExcluiStatusEmUso() {
        when(statusRepository.findById(2L)).thenReturn(Optional.of(new Status("Em andamento", false)));
        when(tarefaRepository.countByStatusId(2L)).thenReturn(4L);

        assertThatThrownBy(() -> statusService.excluir(2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("4 tarefas ainda usam");
        verify(statusRepository, never()).delete(any());
    }

    @Test
    void naoExcluiOStatusFinalizador() {
        when(statusRepository.findById(3L)).thenReturn(Optional.of(new Status("Concluída", true)));

        assertThatThrownBy(() -> statusService.excluir(3L)).isInstanceOf(RegraNegocioException.class);
        verify(statusRepository, never()).delete(any());
    }

    @Test
    void naoDeixaFicarSemStatusFinalizador() {
        when(statusRepository.findById(3L)).thenReturn(Optional.of(new Status("Concluída", true)));
        when(statusRepository.existeNome(anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> statusService.atualizar(3L, form("Concluída", false)))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        erro -> assertThat(erro.getCampo()).isEqualTo("finaliza"));
    }

    @Test
    void novoFinalizadorDesmarcaOAnterior() {
        Status emAndamento = new Status("Em andamento", false);
        when(statusRepository.findById(2L)).thenReturn(Optional.of(emAndamento));
        when(statusRepository.existeNome(anyString(), any())).thenReturn(false);
        when(statusRepository.saveAndFlush(emAndamento)).thenReturn(emAndamento);

        statusService.atualizar(2L, form("Em andamento", true));

        verify(statusRepository).desmarcarFinaliza();
        assertThat(emAndamento.isFinaliza()).isTrue();
    }

    private static StatusForm form(String nome, boolean finaliza) {
        StatusForm form = new StatusForm();
        form.setNome(nome);
        form.setFinaliza(finaliza);
        return form;
    }
}
