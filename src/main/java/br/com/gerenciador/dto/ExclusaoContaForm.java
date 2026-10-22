package br.com.gerenciador.dto;

import jakarta.validation.constraints.NotBlank;

/** "Excluir minha conta": pede a senha para confirmar. */
public class ExclusaoContaForm {

    @NotBlank(message = "Digite sua senha para confirmar.")
    private String senha;

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
