package br.com.gerenciador.service;

import br.com.gerenciador.model.Status;

import java.util.List;

/** Cartões do topo da lista de tarefas: quantas tarefas o usuário tem em cada status e quantas estão atrasadas. */
public record ResumoTarefas(List<ComTotal<Status>> porStatus, long atrasadas) {
}
