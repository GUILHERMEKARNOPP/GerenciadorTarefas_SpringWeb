package br.com.gerenciador.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

/** Números de página mostrados na paginação (${@paginacao.janela(pagina)}): a atual e até 2 de cada lado. */
@Component("paginacao")
public class Paginacao {

    private static final int VIZINHAS = 2;

    public List<Integer> janela(Page<?> pagina) {
        if (pagina.getTotalPages() == 0) {
            return List.of();
        }
        int ultima = pagina.getTotalPages() - 1;
        int atual = Math.min(pagina.getNumber(), ultima);
        return IntStream.rangeClosed(Math.max(0, atual - VIZINHAS), Math.min(ultima, atual + VIZINHAS))
                .boxed()
                .toList();
    }
}
