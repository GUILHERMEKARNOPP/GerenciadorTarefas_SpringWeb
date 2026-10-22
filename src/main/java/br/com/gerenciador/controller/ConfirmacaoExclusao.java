package br.com.gerenciador.controller;

import java.util.List;

/**
 * Dados da tela "Excluir ...?" (template confirmar-exclusao), igual para todos os cadastros.
 *
 * @param entidade nome do registro no singular ("tarefa", "categoria"...)
 * @param menu     item do menu que fica destacado
 * @param lista    texto do link de volta para a listagem
 * @param urlLista endereço da listagem (Cancelar)
 * @param urlAcao  endereço do POST que exclui
 * @param dados    dados do registro mostrados para conferência
 * @param aviso    consequência da exclusão (ex.: tarefas que ficam sem categoria), ou null
 */
public record ConfirmacaoExclusao(String entidade, String menu, String lista, String urlLista, String urlAcao,
                                  List<Campo> dados, String aviso) {

    public record Campo(String rotulo, String valor) {
    }
}
