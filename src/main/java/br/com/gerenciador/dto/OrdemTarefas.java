package br.com.gerenciador.dto;

public enum OrdemTarefas {

    VENCIMENTO("Vencimento"),
    PRIORIDADE("Prioridade"),
    TITULO("Título"),
    RECENTES("Mais recentes");

    private final String descricao;

    OrdemTarefas(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
