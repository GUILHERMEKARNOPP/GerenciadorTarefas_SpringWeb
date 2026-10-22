package br.com.gerenciador.security;

import br.com.gerenciador.model.Usuario;
import br.com.gerenciador.repository.UsuarioRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proteção contra força bruta: conta as senhas erradas seguidas e bloqueia o login por um tempo
 * ao atingir o limite. Um login certo zera a contagem.
 */
@Component
public class BloqueioLogin {

    private final UsuarioRepository usuarioRepository;

    public BloqueioLogin(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @EventListener
    @Transactional
    public void aoErrarSenha(AuthenticationFailureBadCredentialsEvent evento) {
        // Login inexistente não atualiza nenhuma linha, e a tela mostra a mesma mensagem nos dois casos.
        usuarioRepository.registrarFalhaLogin(evento.getAuthentication().getName(),
                Usuario.MAX_TENTATIVAS_LOGIN, (int) Usuario.TEMPO_BLOQUEIO_LOGIN.toSeconds());
    }

    @EventListener
    @Transactional
    public void aoEntrar(AuthenticationSuccessEvent evento) {
        if (evento.getAuthentication().getPrincipal() instanceof UsuarioAutenticado usuario) {
            usuarioRepository.registrarLoginComSucesso(usuario.getId());
        }
    }
}
