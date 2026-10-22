package br.com.gerenciador.controller;

import br.com.gerenciador.dto.DadosPessoaisForm;
import br.com.gerenciador.dto.ExclusaoContaForm;
import br.com.gerenciador.dto.SenhaForm;
import br.com.gerenciador.model.Usuario;
import br.com.gerenciador.security.SessoesUsuario;
import br.com.gerenciador.security.UsuarioAutenticado;
import br.com.gerenciador.service.ContaService;
import br.com.gerenciador.service.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** "Meu perfil": dados, troca de senha e exclusão da própria conta. Sempre do usuário logado. */
@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final ContaService contaService;
    private final SessoesUsuario sessoesUsuario;

    public PerfilController(ContaService contaService, SessoesUsuario sessoesUsuario) {
        this.contaService = contaService;
        this.sessoesUsuario = sessoesUsuario;
    }

    @GetMapping
    public String exibir(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        return pagina(model, usuario.getId());
    }

    @PostMapping
    public String salvarDados(@Valid @ModelAttribute("dados") DadosPessoaisForm form, BindingResult resultado,
                              @AuthenticationPrincipal UsuarioAutenticado usuario, Model model,
                              RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                contaService.atualizarDados(usuario.getId(), form);
                redirect.addFlashAttribute("sucesso", "Dados salvos.");
                return "redirect:/perfil";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return pagina(model, usuario.getId());
    }

    @PostMapping("/senha")
    public String alterarSenha(@Valid @ModelAttribute("senhaForm") SenhaForm form, BindingResult resultado,
                               @AuthenticationPrincipal UsuarioAutenticado usuario, HttpServletRequest requisicao,
                               Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                contaService.alterarSenha(usuario.getId(), form);
                // Quem estava logado com a senha antiga em outro lugar precisa entrar de novo.
                sessoesUsuario.encerrar(usuario.getId(), requisicao.getSession().getId());
                redirect.addFlashAttribute("sucesso", "Senha alterada. As outras sessões abertas com a sua conta foram encerradas.");
                return "redirect:/perfil";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return pagina(model, usuario.getId());
    }

    @PostMapping("/excluir")
    public String excluirConta(@Valid @ModelAttribute("exclusao") ExclusaoContaForm form, BindingResult resultado,
                               @AuthenticationPrincipal UsuarioAutenticado usuario, Authentication autenticacao,
                               HttpServletRequest requisicao, HttpServletResponse resposta, Model model) {
        if (!resultado.hasErrors()) {
            try {
                contaService.excluirConta(usuario.getId(), form.getSenha());
                sessoesUsuario.encerrar(usuario.getId(), null);
                new SecurityContextLogoutHandler().logout(requisicao, resposta, autenticacao);
                return "redirect:/login?contaExcluida";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return pagina(model, usuario.getId());
    }

    // Cada formulário da página tem seu próprio objeto; o que voltou com erro já está no model e é mantido.
    private String pagina(Model model, Long usuarioId) {
        Usuario conta = contaService.buscar(usuarioId);
        model.addAttribute("conta", conta);
        model.addAttribute("totalTarefas", contaService.contarTarefas(usuarioId));
        model.addAttribute("totalCategorias", contaService.contarCategorias(usuarioId));
        if (!model.containsAttribute("dados")) {
            model.addAttribute("dados", DadosPessoaisForm.de(conta));
        }
        if (!model.containsAttribute("senhaForm")) {
            model.addAttribute("senhaForm", new SenhaForm());
        }
        if (!model.containsAttribute("exclusao")) {
            model.addAttribute("exclusao", new ExclusaoContaForm());
        }
        return "perfil";
    }
}
