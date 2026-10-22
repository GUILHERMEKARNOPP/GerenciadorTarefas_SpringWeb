package br.com.gerenciador.security;

import br.com.gerenciador.model.Perfil;
import br.com.gerenciador.model.Usuario;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Usuário logado guardado na sessão. Os controllers pegam o id daqui
 * (nunca de um campo do formulário) para filtrar os dados pelo dono.
 */
public class UsuarioAutenticado implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String login;
    private final Perfil perfil;
    private final boolean bloqueado;
    private String senhaHash;

    public UsuarioAutenticado(Usuario usuario, OffsetDateTime agora) {
        this.id = usuario.getId();
        this.login = usuario.getLogin();
        this.perfil = usuario.getPerfil();
        this.bloqueado = usuario.estaBloqueado(agora);
        this.senhaHash = usuario.getSenhaHash();
    }

    public Long getId() {
        return id;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado;
    }

    /** Depois do login o hash não fica guardado na sessão. */
    @Override
    public void eraseCredentials() {
        senhaHash = null;
    }

    // O registro de sessões agrupa as sessões por usuário usando equals/hashCode.
    @Override
    public boolean equals(Object outro) {
        return outro instanceof UsuarioAutenticado usuario && Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
