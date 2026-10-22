package br.com.gerenciador.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Tela "Criar conta". As regras da senha ficam em PoliticaSenha (valem também para o admin e o perfil). */
public class CadastroForm extends DadosPessoaisForm {

    @NotNull(message = "Informe o usuário.")
    @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
            message = "O usuário deve ter de 3 a 50 caracteres: letras, números, ponto, hífen ou sublinhado.")
    private String login;

    private String senha;

    private String confirmacaoSenha;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
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
