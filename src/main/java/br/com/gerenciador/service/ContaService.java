package br.com.gerenciador.service;

import br.com.gerenciador.dto.CadastroForm;
import br.com.gerenciador.dto.DadosPessoaisForm;
import br.com.gerenciador.dto.SenhaForm;
import br.com.gerenciador.model.Perfil;
import br.com.gerenciador.model.Usuario;
import br.com.gerenciador.repository.CategoriaRepository;
import br.com.gerenciador.repository.TarefaRepository;
import br.com.gerenciador.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

/** A própria conta: "Criar conta" e "Meu perfil". Sempre age sobre o usuário logado. */
@Service
@Transactional(readOnly = true)
public class ContaService {

    private final UsuarioRepository usuarioRepository;
    private final TarefaRepository tarefaRepository;
    private final CategoriaRepository categoriaRepository;
    private final CadastroUsuarios cadastroUsuarios;
    private final PasswordEncoder passwordEncoder;

    public ContaService(UsuarioRepository usuarioRepository, TarefaRepository tarefaRepository,
                        CategoriaRepository categoriaRepository, CadastroUsuarios cadastroUsuarios,
                        PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.tarefaRepository = tarefaRepository;
        this.categoriaRepository = categoriaRepository;
        this.cadastroUsuarios = cadastroUsuarios;
        this.passwordEncoder = passwordEncoder;
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

    /** A primeira conta criada no sistema vira administradora; as demais são usuários comuns. */
    @Transactional
    public Usuario cadastrar(CadastroForm form) {
        String login = form.getLogin().strip();
        String email = form.getEmail().strip();
        PoliticaSenha.validar(form.getSenha(), form.getConfirmacaoSenha(), login);
        cadastroUsuarios.verificarDisponivel(login, email, null);
        Perfil perfil = usuarioRepository.count() == 0 ? Perfil.ADMIN : Perfil.USUARIO;
        Usuario usuario = new Usuario(form.getNome().strip(), email, login, passwordEncoder.encode(form.getSenha()), perfil);
        return cadastroUsuarios.salvar(usuario);
    }

    @Transactional
    public void atualizarDados(Long id, DadosPessoaisForm form) {
        Usuario usuario = buscar(id);
        String email = form.getEmail().strip();
        cadastroUsuarios.verificarDisponivel(usuario.getLogin(), email, id);
        usuario.setNome(form.getNome().strip());
        usuario.setEmail(email);
        cadastroUsuarios.salvar(usuario);
    }

    // noRollbackFor: a tentativa errada precisa ficar registrada mesmo com a exceção (limita chutes de senha).
    @Transactional(noRollbackFor = RegraNegocioException.class)
    public void alterarSenha(Long id, SenhaForm form) {
        Usuario usuario = buscar(id);
        conferirSenha(usuario, form.getSenhaAtual(), "senhaAtual");
        PoliticaSenha.validar(form.getSenha(), form.getConfirmacaoSenha(), usuario.getLogin());
        usuario.setSenhaHash(passwordEncoder.encode(form.getSenha()));
    }

    @Transactional(noRollbackFor = RegraNegocioException.class)
    public void excluirConta(Long id, String senha) {
        Usuario usuario = buscar(id);
        conferirSenha(usuario, senha, "senha");
        if (usuario.isAdmin() && usuarioRepository.countByPerfil(Perfil.ADMIN) <= 1) {
            throw new RegraNegocioException("Você é o único administrador. Promova outro usuário a administrador "
                    + "antes de excluir sua conta.");
        }
        usuarioRepository.delete(usuario);
    }

    /** Senha errada conta como tentativa de login: 5 erros seguidos bloqueiam a conta por 5 minutos. */
    private void conferirSenha(Usuario usuario, String senha, String campo) {
        if (usuario.estaBloqueado(OffsetDateTime.now())) {
            throw new RegraNegocioException(campo, "Muitas tentativas incorretas. Tente novamente em alguns minutos.");
        }
        boolean confere = senha != null
                && senha.getBytes(StandardCharsets.UTF_8).length <= 72
                && passwordEncoder.matches(senha, usuario.getSenhaHash());
        if (!confere) {
            usuarioRepository.registrarFalhaLogin(usuario.getLogin(), Usuario.MAX_TENTATIVAS_LOGIN,
                    (int) Usuario.TEMPO_BLOQUEIO_LOGIN.toSeconds());
            throw new RegraNegocioException(campo, "Senha incorreta.");
        }
    }
}
