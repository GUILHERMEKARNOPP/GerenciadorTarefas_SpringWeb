package br.com.gerenciador.dto;

import br.com.gerenciador.model.Perfil;
import br.com.gerenciador.model.Usuario;
import jakarta.validation.constraints.NotNull;

/** Cadastro/edição de usuário pelo administrador. Na edição a senha é opcional (em branco = mantém). */
public class UsuarioForm extends CadastroForm {

    @NotNull(message = "Escolha o perfil.")
    private Perfil perfil = Perfil.USUARIO;

    private boolean desbloquear;

    public static UsuarioForm de(Usuario usuario) {
        UsuarioForm form = new UsuarioForm();
        form.setNome(usuario.getNome());
        form.setEmail(usuario.getEmail());
        form.setLogin(usuario.getLogin());
        form.perfil = usuario.getPerfil();
        return form;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean isDesbloquear() {
        return desbloquear;
    }

    public void setDesbloquear(boolean desbloquear) {
        this.desbloquear = desbloquear;
    }
}
