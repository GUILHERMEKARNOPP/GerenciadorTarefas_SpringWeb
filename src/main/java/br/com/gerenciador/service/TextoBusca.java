package br.com.gerenciador.service;

import java.util.Locale;

/** Monta o padrão LIKE das pesquisas por texto (as consultas usam "escape '!'"). */
final class TextoBusca {

    private TextoBusca() {
    }

    /** "50%" vira "%50!%%": o % digitado é procurado literalmente, não como curinga. Vazio = null (sem filtro). */
    static String padraoLike(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String escapado = texto.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escapado + "%";
    }
}
