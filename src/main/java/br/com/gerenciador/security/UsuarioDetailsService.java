package br.com.gerenciador.security;

import br.com.gerenciador.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/** Busca o usuário pelo login (sem diferenciar maiúsculas) para o Spring Security conferir a senha. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) {
        // A mensagem não chega à tela: o Spring Security troca por "usuário ou senha inválidos".
        return usuarioRepository.buscarPorLogin(login.strip())
                .map(usuario -> new UsuarioAutenticado(usuario, OffsetDateTime.now()))
                .orElseThrow(() -> new UsernameNotFoundException("Login não encontrado."));
    }
}
