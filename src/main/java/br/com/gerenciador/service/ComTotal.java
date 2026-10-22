package br.com.gerenciador.service;

/** Item de uma listagem junto com quantas tarefas usam esse item. */
public record ComTotal<T>(T item, long tarefas) {
}
