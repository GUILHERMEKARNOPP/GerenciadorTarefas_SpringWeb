package br.com.gerenciador.controller;

import br.com.gerenciador.model.Tema;
import br.com.gerenciador.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** "Escolha o tema": aparece antes do primeiro login e depois pelo menu "Tema". */
@Controller
public class TemaController {

    @GetMapping("/tema")
    public String escolher(@CookieValue(name = CookieTema.NOME, required = false) String cookie, Model model) {
        model.addAttribute("temas", Tema.values());
        // Na primeira visita nada foi escolhido ainda, então nenhum mundo aparece como "Tema atual".
        model.addAttribute("temaEscolhido", CookieTema.ler(cookie).orElse(null));
        return "tema";
    }

    @PostMapping("/tema")
    public String salvar(@RequestParam("tema") String slug, @AuthenticationPrincipal UsuarioAutenticado usuario,
                         HttpServletRequest requisicao, HttpServletResponse resposta) {
        Tema escolhido = Tema.doSlug(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
        CookieTema.gravar(requisicao, resposta, escolhido);
        return usuario != null ? "redirect:/tarefas" : "redirect:/login";
    }
}
