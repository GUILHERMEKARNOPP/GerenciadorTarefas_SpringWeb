package br.com.gerenciador.security;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

/**
 * Encerra as sessões abertas de um usuário depois de mudanças que afetam o acesso dele
 * (troca de senha, mudança de perfil, exclusão). Quem estava logado volta para a tela de login.
 */
@Component
public class SessoesUsuario {

    private final SessionRegistry sessionRegistry;

    public SessoesUsuario(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /** @param sessaoMantida id da sessão que continua aberta (a de quem fez a mudança), ou null para encerrar todas. */
    public void encerrar(Long usuarioId, String sessaoMantida) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> principal instanceof UsuarioAutenticado usuario && usuario.getId().equals(usuarioId))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .filter(sessao -> !sessao.getSessionId().equals(sessaoMantida))
                .forEach(SessionInformation::expireNow);
    }
}
