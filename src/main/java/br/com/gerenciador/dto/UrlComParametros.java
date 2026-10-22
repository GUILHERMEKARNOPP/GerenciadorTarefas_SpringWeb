package br.com.gerenciador.dto;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/** Monta links de listagem mantendo os filtros (usado na paginação). */
final class UrlComParametros {

    private UrlComParametros() {
    }

    static String montar(String caminho, Map<String, Object> parametros) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath(caminho);
        parametros.keySet().forEach(nome -> url.queryParam(nome, "{" + nome + "}"));
        // encode() + buildAndExpand codifica os valores por completo (inclusive &, = e +) digitados na pesquisa.
        return url.encode().buildAndExpand(parametros).toUriString();
    }
}
