package br.com.gerenciador.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Resultado de um "count ... group by id" (ex.: quantas tarefas cada status tem). */
public record ContagemPorId(Long id, Long total) {

    public static Map<Long, Long> comoMapa(List<ContagemPorId> contagens) {
        return contagens.stream().collect(Collectors.toMap(ContagemPorId::id, ContagemPorId::total));
    }
}
