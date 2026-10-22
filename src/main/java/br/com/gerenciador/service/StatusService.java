package br.com.gerenciador.service;

import br.com.gerenciador.dto.StatusForm;
import br.com.gerenciador.model.Status;
import br.com.gerenciador.repository.ContagemPorId;
import br.com.gerenciador.repository.StatusRepository;
import br.com.gerenciador.repository.TarefaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Status das tarefas (área do administrador). Sempre existe exatamente um status que finaliza a tarefa. */
@Service
@Transactional(readOnly = true)
public class StatusService {

    private final StatusRepository statusRepository;
    private final TarefaRepository tarefaRepository;

    public StatusService(StatusRepository statusRepository, TarefaRepository tarefaRepository) {
        this.statusRepository = statusRepository;
        this.tarefaRepository = tarefaRepository;
    }

    public List<Status> listar() {
        return statusRepository.findAllByOrderByIdAsc();
    }

    public List<ComTotal<Status>> listarComTotais() {
        Map<Long, Long> porStatus = ContagemPorId.comoMapa(tarefaRepository.contarPorStatus());
        return listar().stream()
                .map(status -> new ComTotal<>(status, porStatus.getOrDefault(status.getId(), 0L)))
                .toList();
    }

    public Status buscar(Long id) {
        return statusRepository.findById(id).orElseThrow(RecursoNaoEncontradoException::new);
    }

    public long contarTarefas(Long id) {
        return tarefaRepository.countByStatusId(buscar(id).getId());
    }

    @Transactional
    public Status criar(StatusForm form) {
        return salvar(new Status(), form);
    }

    @Transactional
    public void atualizar(Long id, StatusForm form) {
        salvar(buscar(id), form);
    }

    @Transactional
    public void excluir(Long id) {
        Status status = buscar(id);
        if (status.isFinaliza()) {
            throw new RegraNegocioException("Não é possível excluir o status “" + status.getNome()
                    + "”: ele é o que finaliza as tarefas. Marque outro status como finalizador antes.");
        }
        long emUso = tarefaRepository.countByStatusId(id);
        if (emUso > 0) {
            throw emUso(status, emUso);
        }
        try {
            statusRepository.delete(status);
            statusRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // Uma tarefa pode ter passado a usar o status depois da contagem acima (FK com ON DELETE RESTRICT).
            // Não dá para contar de novo: depois do erro o PostgreSQL recusa outras consultas na transação.
            throw new RegraNegocioException("Não foi possível excluir o status “" + status.getNome()
                    + "”: há tarefas usando este status.");
        }
    }

    private Status salvar(Status status, StatusForm form) {
        String nome = form.getNome().strip();
        if (statusRepository.existeNome(nome, status.getId())) {
            throw new RegraNegocioException("nome", "Já existe um status chamado “" + nome + "”.");
        }
        if (status.isFinaliza() && !form.isFinaliza()) {
            throw new RegraNegocioException("finaliza", "Sempre precisa existir um status que finaliza a tarefa. "
                    + "Para trocar, marque essa opção em outro status.");
        }
        if (form.isFinaliza() && !status.isFinaliza()) {
            // Só um status finaliza a tarefa: o novo finalizador tira a marcação do anterior.
            statusRepository.desmarcarFinaliza();
        }
        status.setNome(nome);
        status.setFinaliza(form.isFinaliza());
        try {
            return statusRepository.saveAndFlush(status);
        } catch (DataIntegrityViolationException e) {
            throw new RegraNegocioException("nome", "Já existe um status chamado “" + nome + "”.");
        }
    }

    private static RegraNegocioException emUso(Status status, long tarefas) {
        return new RegraNegocioException("Não foi possível excluir o status “" + status.getNome() + "”: "
                + tarefas + (tarefas == 1 ? " tarefa ainda usa" : " tarefas ainda usam")
                + " este status. Altere essas tarefas antes de excluir.");
    }
}
