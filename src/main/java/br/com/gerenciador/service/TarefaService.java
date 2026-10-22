package br.com.gerenciador.service;

import br.com.gerenciador.dto.FiltroTarefas;
import br.com.gerenciador.dto.OrdemTarefas;
import br.com.gerenciador.dto.TarefaForm;
import br.com.gerenciador.model.Status;
import br.com.gerenciador.model.Tarefa;
import br.com.gerenciador.repository.CategoriaRepository;
import br.com.gerenciador.repository.ContagemPorId;
import br.com.gerenciador.repository.PrioridadeRepository;
import br.com.gerenciador.repository.StatusRepository;
import br.com.gerenciador.repository.TarefaRepository;
import br.com.gerenciador.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Tarefas do usuário logado. Todo método recebe o id do dono e só enxerga as tarefas dele. */
@Service
@Transactional(readOnly = true)
public class TarefaService {

    static final int TAREFAS_POR_PAGINA = 10;

    private final TarefaRepository tarefaRepository;
    private final CategoriaRepository categoriaRepository;
    private final StatusRepository statusRepository;
    private final PrioridadeRepository prioridadeRepository;
    private final UsuarioRepository usuarioRepository;

    public TarefaService(TarefaRepository tarefaRepository, CategoriaRepository categoriaRepository,
                         StatusRepository statusRepository, PrioridadeRepository prioridadeRepository,
                         UsuarioRepository usuarioRepository) {
        this.tarefaRepository = tarefaRepository;
        this.categoriaRepository = categoriaRepository;
        this.statusRepository = statusRepository;
        this.prioridadeRepository = prioridadeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Page<Tarefa> buscar(Long usuarioId, FiltroTarefas filtro) {
        PageRequest pagina = PageRequest.of(filtro.getPagina(), TAREFAS_POR_PAGINA, ordenacao(filtro.getOrdem()));
        return tarefaRepository.buscar(usuarioId, filtro.getStatus(), filtro.getPrioridade(), filtro.getCategoria(),
                TextoBusca.padraoLike(filtro.getQ()), pagina);
    }

    public ResumoTarefas resumir(Long usuarioId, LocalDate hoje) {
        Map<Long, Long> porStatus = ContagemPorId.comoMapa(tarefaRepository.contarPorStatusDoUsuario(usuarioId));
        List<ComTotal<Status>> cartoes = statusRepository.findAllByOrderByIdAsc().stream()
                .map(status -> new ComTotal<>(status, porStatus.getOrDefault(status.getId(), 0L)))
                .toList();
        return new ResumoTarefas(cartoes, tarefaRepository.contarAtrasadas(usuarioId, hoje));
    }

    public Tarefa buscarDoUsuario(Long id, Long usuarioId) {
        return tarefaRepository.buscarDoUsuario(id, usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
    }

    @Transactional
    public Tarefa criar(Long usuarioId, TarefaForm form) {
        Tarefa tarefa = new Tarefa(usuarioRepository.getReferenceById(usuarioId));
        preencher(tarefa, form, usuarioId);
        return tarefaRepository.save(tarefa);
    }

    @Transactional
    public void atualizar(Long id, Long usuarioId, TarefaForm form) {
        preencher(buscarDoUsuario(id, usuarioId), form, usuarioId);
    }

    @Transactional
    public void concluir(Long id, Long usuarioId) {
        Tarefa tarefa = buscarDoUsuario(id, usuarioId);
        Status statusFinal = statusRepository.findByFinalizaTrue().orElseThrow(() -> new RegraNegocioException(
                "Nenhum status está marcado para finalizar tarefas. Avise o administrador."));
        tarefa.concluir(statusFinal);
    }

    @Transactional
    public void excluir(Long id, Long usuarioId) {
        tarefaRepository.delete(buscarDoUsuario(id, usuarioId));
    }

    private void preencher(Tarefa tarefa, TarefaForm form, Long usuarioId) {
        tarefa.setTitulo(form.getTitulo().strip());
        tarefa.setDescricao(form.getDescricao() == null || form.getDescricao().isBlank() ? null : form.getDescricao().strip());
        tarefa.setDataVencimento(form.getDataVencimento());
        // A categoria vem do formulário: confere que é do próprio usuário antes de usar.
        tarefa.setCategoria(form.getCategoriaId() == null ? null
                : categoriaRepository.findByIdAndUsuarioId(form.getCategoriaId(), usuarioId)
                        .orElseThrow(() -> new RegraNegocioException("categoriaId", "Categoria inválida.")));
        tarefa.setStatus(statusRepository.findById(form.getStatusId())
                .orElseThrow(() -> new RegraNegocioException("statusId", "Status inválido.")));
        tarefa.setPrioridade(prioridadeRepository.findById(form.getPrioridadeId())
                .orElseThrow(() -> new RegraNegocioException("prioridadeId", "Prioridade inválida.")));
    }

    private static Sort ordenacao(OrdemTarefas ordem) {
        Sort.Order vencimento = Sort.Order.asc("dataVencimento").nullsLast();
        return switch (ordem == null ? OrdemTarefas.VENCIMENTO : ordem) {
            case VENCIMENTO -> Sort.by(vencimento, Sort.Order.asc("id"));
            case PRIORIDADE -> Sort.by(Sort.Order.asc("prioridade.nivel"), vencimento, Sort.Order.asc("id"));
            case TITULO -> Sort.by(Sort.Order.asc("titulo"), Sort.Order.asc("id"));
            case RECENTES -> Sort.by(Sort.Order.desc("dataCriacao"), Sort.Order.desc("id"));
        };
    }
}
