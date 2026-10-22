package br.com.gerenciador.service;

import br.com.gerenciador.model.Usuario;
import br.com.gerenciador.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** Regras de unicidade de login/e-mail, comuns ao cadastro pela tela "Criar conta" e pelo administrador. */
@Component
class CadastroUsuarios {

    private final UsuarioRepository usuarioRepository;

    CadastroUsuarios(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** @param ignorarId usuário que está sendo editado (pode manter o próprio login/e-mail), ou null no cadastro. */
    void verificarDisponivel(String login, String email, Long ignorarId) {
        if (usuarioRepository.existeLogin(login, ignorarId)) {
            throw new RegraNegocioException("login", "Este usuário já está em uso. Escolha outro.");
        }
        if (usuarioRepository.existeEmail(email, ignorarId)) {
            throw new RegraNegocioException("email", "Este e-mail já está cadastrado.");
        }
    }

    Usuario salvar(Usuario usuario) {
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Outro cadastro igual pode ter entrado entre a checagem e o INSERT (índices únicos do banco).
            throw new RegraNegocioException("Usuário ou e-mail já cadastrado.");
        }
    }
}
