package br.com.gerenciador.controller;

import br.com.gerenciador.model.Tema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Optional;

/** Cookie que lembra o tema escolhido neste navegador. Sem ele, a pessoa ainda não passou pela escolha. */
final class CookieTema {

    static final String NOME = "tema";
    private static final Duration VALIDADE = Duration.ofDays(365);

    private CookieTema() {
    }

    /** Cookie ausente ou com valor fora da lista conta como "ainda não escolheu". */
    static Optional<Tema> ler(String valor) {
        return Tema.doSlug(valor);
    }

    static void gravar(HttpServletRequest requisicao, HttpServletResponse resposta, Tema tema) {
        String caminho = requisicao.getContextPath().isEmpty() ? "/" : requisicao.getContextPath();
        ResponseCookie cookie = ResponseCookie.from(NOME, tema.getSlug())
                .path(caminho)
                .maxAge(VALIDADE)
                .httpOnly(true)
                .secure(requisicao.isSecure())
                .sameSite("Lax")
                .build();
        resposta.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
