package br.com.gerenciador.service;

import br.com.gerenciador.dto.UsuarioForm;
import br.com.gerenciador.model.Perfil;
import br.com.gerenciador.model.Usuario;
import br.com.gerenciador.repository.CategoriaRepository;
import br.com.gerenciador.repository.ContagemPorId;
import br.com.gerenciador.repository.TarefaRepository;
import br.com.gerenciador.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Manutenção de usuários pelo administrador. Sempre sobra pelo menos um ADMIN no sistema. */
@Service
@Transactional(readOnly = true)
public class UsuarioAdminService {

    static final int USUARIOS_POR_PAGINA = 10;

    private final UsuarioRepository usuarioRepository;
    private final TarefaRepository tarefaRepository;
    private final CategoriaRepository categoriaRepository;
    private final CadastroUsuarios cadastroUsuarios;
    private final PasswordEncoder passwordEncoder;

    public UsuarioAdminService(UsuarioRepository usuarioRepository, TarefaRepository tarefaRepository,
                               CategoriaRepository categoriaRepository, CadastroUsuarios cadastroUsuarios,
                               PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.tarefaRepository = tarefaRepository;
        this.categoriaRepository = categoriaRepository;
        this.cadastroUsuarios = cadastroUsuarios;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<ComTotal<Usuario>> buscar(String texto, Perfil perfil, int pagina) {
        PageRequest pageRequest = PageRequest.of(Math.max(pagina, 0), USUARIOS_POR_PAGINA,
                Sort.by(Sort.Order.asc("nome"), Sort.Order.asc("id")));
        Page<Usuario> usuarios = usuarioRepository.buscar(TextoBusca.padraoLike(texto), perfil, pageRequest);
        List<Long> ids = usuarios.map(Usuario::getId).toList();
        // Uma consulta só para os totais da página inteira (em vez de uma por usuário).
        Map<Long, Long> tarefas = ids.isEmpty() ? Map.of() : ContagemPorId.comoMapa(tarefaRepository.contarPorUsuario(ids));
        return usuarios.map(usuario -> new ComTotal<>(usuario, tarefas.getOrDefault(usuario.getId(), 0L)));
    }

    public Usuario buscar(Long id) {
        return usuarioRepository.findById(id).orElseThrow(RecursoNaoEncontradoException::new);
    }

    public long contarTarefas(Long id) {
        return tarefaRepository.countByUsuarioId(id);
    }

    public long contarCategorias(Long id) {
        return categoriaRepository.countByUsuarioId(id);
    }

    @Transactional
    public Usuario criar(UsuarioForm form) {
        String login = form.getLogin().strip();
        String email = form.getEmail().strip();
        if (form.getSenha() == null || form.getSenha().isEmpty()) {
            throw new RegraNegocioException("senha", "Informe a senha do novo usuário.");
        }
        PoliticaSenha.validar(form.getSenha(), form.getConfirmacaoSenha(), login);
        cadastroUsuarios.verificarDisponivel(login, email, null);
        Usuario usuario = new Usuario(form.getNome().strip(), email, login,
                passwordEncoder.encode(form.getSenha()), form.getPerfil());
        return cadastroUsuarios.salvar(usuario);
    }

    /** @param idAdminLogado quem está editando: não pode tirar o próprio perfil de administrador. */
    @Transactional
    public void atualizar(Long id, Long idAdminLogado, UsuarioForm form) {
        Usuario usuario = buscar(id);
        String login = form.getLogin().strip();
        String email = form.getEmail().strip();
        if (usuario.isAdmin() && form.getPerfil() != Perfil.ADMIN) {
            if (id.equals(idAdminLogado)) {
                throw new RegraNegocioException("perfil", "Você não pode tirar o seu próprio perfil de administrador.");
            }
            exigirOutroAdmin();
        }
        boolean trocarSenha = form.getSenha() != null && !form.getSenha().isEmpty();
        if (trocarSenha) {
            PoliticaSenha.validar(form.getSenha(), form.getConfirmacaoSenha(), login);
        }
        cadastroUsuarios.verificarDisponivel(login, email, id);

        usuario.setNome(form.getNome().strip());
        usuario.setEmail(email);
        usuario.setLogin(login);
        usuario.setPerfil(form.getPerfil());
        if (trocarSenha) {
            usuario.setSenhaHash(passwordEncoder.encode(form.getSenha()));
        }
        if (form.isDesbloquear()) {
            usuario.desbloquear();
        }
        cadastroUsuarios.salvar(usuario);
    }

    /** As tarefas e categorias do usuário são apagadas junto (ON DELETE CASCADE no banco). */
    @Transactional
    public void excluir(Long id, Long idAdminLogado) {
        if (id.equals(idAdminLogado)) {
            throw new RegraNegocioException("Você não pode excluir a própria conta por aqui. Use a tela Meu perfil.");
        }
        Usuario usuario = buscar(id);
        if (usuario.isAdmin()) {
            exigirOutroAdmin();
        }
        usuarioRepository.delete(usuario);
    }

    private void exigirOutroAdmin() {
        if (usuarioRepository.countByPerfil(Perfil.ADMIN) <= 1) {
            throw new RegraNegocioException("perfil",
                    "Este é o único administrador do sistema. Promova outro usuário a administrador antes.");
        }
    }
}
