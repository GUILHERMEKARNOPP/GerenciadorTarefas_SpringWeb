package br.com.gerenciador.service;

import br.com.gerenciador.dto.TarefaForm;
import br.com.gerenciador.model.Perfil;
import br.com.gerenciador.model.Status;
import br.com.gerenciador.model.Tarefa;
import br.com.gerenciador.model.Usuario;
import br.com.gerenciador.repository.CategoriaRepository;
import br.com.gerenciador.repository.PrioridadeRepository;
import br.com.gerenciador.repository.StatusRepository;
import br.com.gerenciador.repository.TarefaRepository;
import br.com.gerenciador.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    private static final Long ANA = 1L;

    @Mock
    private TarefaRepository tarefaRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private StatusRepository statusRepository;
    @Mock
    private PrioridadeRepository prioridadeRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private TarefaService tarefaService;

    private final Usuario ana = new Usuario("Ana", "ana@example.com", "ana", "hash", Perfil.USUARIO);

    @Test
    void naoDeixaUsarCategoriaDeOutroUsuario() {
        when(usuarioRepository.getReferenceById(ANA)).thenReturn(ana);
        when(categoriaRepository.findByIdAndUsuarioId(7L, ANA)).thenReturn(Optional.empty());
        TarefaForm form = new TarefaForm();
        form.setTitulo("Tarefa");
        form.setCategoriaId(7L);

        assertThatThrownBy(() -> tarefaService.criar(ANA, form))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        erro -> assertThat(erro.getCampo()).isEqualTo("categoriaId"));
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void concluirUsaOStatusQueFinaliza() {
        Tarefa tarefa = new Tarefa(ana);
        tarefa.setStatus(new Status("Pendente", false));
        Status concluida = new Status("Concluída", true);
        when(tarefaRepository.buscarDoUsuario(5L, ANA)).thenReturn(Optional.of(tarefa));
        when(statusRepository.findByFinalizaTrue()).thenReturn(Optional.of(concluida));

        tarefaService.concluir(5L, ANA);

        assertThat(tarefa.getStatus()).isSameAs(concluida);
    }

    @Test
    void tarefaDeOutroUsuarioNaoEhEncontrada() {
        when(tarefaRepository.buscarDoUsuario(5L, ANA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tarefaService.concluir(5L, ANA)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> tarefaService.excluir(5L, ANA)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(tarefaRepository, never()).delete(any());
    }

    @Test
    void concluirSemStatusFinalizadorAvisaOUsuario() {
        Tarefa tarefa = new Tarefa(ana);
        when(tarefaRepository.buscarDoUsuario(5L, ANA)).thenReturn(Optional.of(tarefa));
        when(statusRepository.findByFinalizaTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tarefaService.concluir(5L, ANA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("administrador");
    }
}
