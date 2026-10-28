package br.com.gerenciador.controller;

import br.com.gerenciador.dto.CadastroForm;
import br.com.gerenciador.model.Usuario;
import br.com.gerenciador.security.UsuarioAutenticado;
import br.com.gerenciador.service.ContaService;
import br.com.gerenciador.service.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Telas abertas a visitantes: login e "Criar conta". O POST do login é tratado pelo Spring Security. */
@Controller
public class AcessoController {

    private final ContaService contaService;

    public AcessoController(ContaService contaService) {
        this.contaService = contaService;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/tarefas";
    }

    /** Na primeira visita (sem tema no cookie), a pessoa escolhe o tema antes de ver o login. */
    @GetMapping("/login")
    public String login(@AuthenticationPrincipal UsuarioAutenticado usuario,
                        @CookieValue(name = CookieTema.NOME, required = false) String tema) {
        if (usuario != null) {
            return "redirect:/tarefas";
        }
        return CookieTema.ler(tema).isPresent() ? "login" : "redirect:/tema";
    }

    @GetMapping("/cadastro")
    public String cadastro(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        if (usuario != null) {
            return "redirect:/tarefas";
        }
        model.addAttribute("cadastro", new CadastroForm());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("cadastro") CadastroForm form, BindingResult resultado,
                            RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                Usuario usuario = contaService.cadastrar(form);
                redirect.addFlashAttribute("sucesso", usuario.isAdmin()
                        ? "Conta de administrador criada! Entre com seu usuário e senha."
                        : "Conta criada! Entre com seu usuário e senha.");
                return "redirect:/login";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return "cadastro";
    }
}
