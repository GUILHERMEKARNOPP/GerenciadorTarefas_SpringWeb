package br.com.gerenciador.controller;

import br.com.gerenciador.model.Tema;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Deixa ${tema} disponível em todas as telas, para o layout carregar o visual escolhido. */
@ControllerAdvice
public class TemaAtual {

    @ModelAttribute("tema")
    public Tema tema(@CookieValue(name = CookieTema.NOME, required = false) String valor) {
        return CookieTema.ler(valor).orElse(Tema.PADRAO);
    }
}
