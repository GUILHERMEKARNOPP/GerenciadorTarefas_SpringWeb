package br.com.gerenciador.service;

import br.com.gerenciador.dto.PrioridadeForm;
import br.com.gerenciador.model.Prioridade;
import br.com.gerenciador.repository.ContagemPorId;
import br.com.gerenciador.repository.PrioridadeRepository;
import br.com.gerenciador.repository.TarefaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Prioridades das tarefas (área do administrador). */
@Service
@Transactional(readOnly = true)
public class PrioridadeService {

    private final PrioridadeRepository prioridadeRepository;
    private final TarefaRepository tarefaRepository;

    public PrioridadeService(PrioridadeRepository prioridadeRepository, TarefaRepository tarefaRepository) {
        this.prioridadeRepository = prioridadeRepository;
        this.tarefaRepository = tarefaRepository;
    }

    public List<Prioridade> listar() {
        return prioridadeRepository.findAllByOrderByNivelAsc();
    }

    public List<ComTotal<Prioridade>> listarComTotais() {
        Map<Long, Long> porPrioridade = ContagemPorId.comoMapa(tarefaRepository.contarPorPrioridade());
        return listar().stream()
                .map(prioridade -> new ComTotal<>(prioridade, porPrioridade.getOrDefault(prioridade.getId(), 0L)))
                .toList();
    }

    public Prioridade buscar(Long id) {
        return prioridadeRepository.findById(id).orElseThrow(RecursoNaoEncontradoException::new);
    }

    public long contarTarefas(Long id) {
        return tarefaRepository.countByPrioridadeId(buscar(id).getId());
    }

    @Transactional
    public Prioridade criar(PrioridadeForm form) {
        return salvar(new Prioridade(), form);
    }

    @Transactional
    public void atualizar(Long id, PrioridadeForm form) {
        salvar(buscar(id), form);
    }

    @Transactional
    public void excluir(Long id) {
        Prioridade prioridade = buscar(id);
        long emUso = tarefaRepository.countByPrioridadeId(id);
        if (emUso > 0) {
            throw emUso(prioridade, emUso);
        }
        try {
            prioridadeRepository.delete(prioridade);
            prioridadeRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // Uma tarefa pode ter passado a usar a prioridade depois da contagem acima (FK com ON DELETE RESTRICT).
            // Não dá para contar de novo: depois do erro o PostgreSQL recusa outras consultas na transação.
            throw new RegraNegocioException("Não foi possível excluir a prioridade “" + prioridade.getNome()
                    + "”: há tarefas usando esta prioridade.");
        }
    }

    private Prioridade salvar(Prioridade prioridade, PrioridadeForm form) {
        String nome = form.getNome().strip();
        if (prioridadeRepository.existeNome(nome, prioridade.getId())) {
            throw new RegraNegocioException("nome", "Já existe uma prioridade chamada “" + nome + "”.");
        }
        if (prioridadeRepository.existeNivel(form.getNivel(), prioridade.getId())) {
            throw new RegraNegocioException("nivel", "Já existe uma prioridade com o nível " + form.getNivel() + ".");
        }
        prioridade.setNome(nome);
        prioridade.setNivel(form.getNivel());
        prioridade.setCor(form.getCor().toLowerCase(Locale.ROOT));
        try {
            return prioridadeRepository.saveAndFlush(prioridade);
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("Já existe uma prioridade com esse nome ou nível.");
        }
    }

    private static RegraNegocioException emUso(Prioridade prioridade, long tarefas) {
        return new RegraNegocioException("Não foi possível excluir a prioridade “" + prioridade.getNome() + "”: "
                + tarefas + (tarefas == 1 ? " tarefa ainda usa" : " tarefas ainda usam")
                + " esta prioridade. Altere essas tarefas antes de excluir.");
    }
}
