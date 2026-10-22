package br.com.gerenciador.service;

import br.com.gerenciador.dto.CategoriaForm;
import br.com.gerenciador.model.Categoria;
import br.com.gerenciador.repository.CategoriaRepository;
import br.com.gerenciador.repository.ContagemPorId;
import br.com.gerenciador.repository.TarefaRepository;
import br.com.gerenciador.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Categorias do usuário logado. Todo método recebe o id do dono e só enxerga as categorias dele. */
@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final TarefaRepository tarefaRepository;
    private final UsuarioRepository usuarioRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, TarefaRepository tarefaRepository,
                            UsuarioRepository usuarioRepository) {
        this.categoriaRepository = categoriaRepository;
        this.tarefaRepository = tarefaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Categoria> listar(Long usuarioId) {
        return categoriaRepository.listarDoUsuario(usuarioId);
    }

    public List<ComTotal<Categoria>> listarComTotais(Long usuarioId) {
        Map<Long, Long> porCategoria = ContagemPorId.comoMapa(tarefaRepository.contarPorCategoria(usuarioId));
        return listar(usuarioId).stream()
                .map(categoria -> new ComTotal<>(categoria, porCategoria.getOrDefault(categoria.getId(), 0L)))
                .toList();
    }

    public Categoria buscarDoUsuario(Long id, Long usuarioId) {
        return categoriaRepository.findByIdAndUsuarioId(id, usuarioId).orElseThrow(RecursoNaoEncontradoException::new);
    }

    public long contarTarefas(Long id, Long usuarioId) {
        return tarefaRepository.countByCategoriaId(buscarDoUsuario(id, usuarioId).getId());
    }

    @Transactional
    public Categoria criar(Long usuarioId, CategoriaForm form) {
        return salvar(new Categoria(usuarioRepository.getReferenceById(usuarioId)), usuarioId, form);
    }

    @Transactional
    public void atualizar(Long id, Long usuarioId, CategoriaForm form) {
        salvar(buscarDoUsuario(id, usuarioId), usuarioId, form);
    }

    /** As tarefas da categoria não são apagadas: ficam "sem categoria" (ON DELETE SET NULL no banco). */
    @Transactional
    public void excluir(Long id, Long usuarioId) {
        categoriaRepository.delete(buscarDoUsuario(id, usuarioId));
    }

    private Categoria salvar(Categoria categoria, Long usuarioId, CategoriaForm form) {
        String nome = form.getNome().strip();
        if (categoriaRepository.existeNome(usuarioId, nome, categoria.getId())) {
            throw new RegraNegocioException("nome", "Você já tem uma categoria chamada “" + nome + "”.");
        }
        categoria.setNome(nome);
        categoria.setDescricao(form.getDescricao() == null || form.getDescricao().isBlank() ? null : form.getDescricao().strip());
        try {
            return categoriaRepository.saveAndFlush(categoria);
        } catch (DataIntegrityViolationException e) {
            // Outro cadastro igual pode ter entrado entre a checagem acima e o INSERT.
            throw new RegraNegocioException("nome", "Você já tem uma categoria chamada “" + nome + "”.");
        }
    }
}
