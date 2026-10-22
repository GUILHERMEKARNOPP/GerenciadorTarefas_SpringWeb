package br.com.gerenciador.model;

/** Perfil de acesso do usuário. ADMIN também mantém usuários, status e prioridades. */
public enum Perfil {

    ADMIN("Administrador"),
    USUARIO("Usuário");

    private final String descricao;

    Perfil(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
