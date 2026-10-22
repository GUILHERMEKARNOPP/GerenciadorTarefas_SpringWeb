package br.com.gerenciador.dto;

import jakarta.validation.constraints.NotBlank;

/** "Alterar senha" em Meu perfil. */
public class SenhaForm {

    @NotBlank(message = "Informe a senha atual.")
    private String senhaAtual;

    private String senha;

    private String confirmacaoSenha;

    public String getSenhaAtual() {
        return senhaAtual;
    }

    public void setSenhaAtual(String senhaAtual) {
        this.senhaAtual = senhaAtual;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }
}
