package br.com.gerenciador.controller.admin;

import br.com.gerenciador.controller.ConfirmacaoExclusao;
import br.com.gerenciador.controller.ConfirmacaoExclusao.Campo;
import br.com.gerenciador.controller.ErrosFormulario;
import br.com.gerenciador.dto.FiltroUsuarios;
import br.com.gerenciador.dto.UsuarioForm;
import br.com.gerenciador.model.Perfil;
import br.com.gerenciador.model.Usuario;
import br.com.gerenciador.security.SessoesUsuario;
import br.com.gerenciador.security.UsuarioAutenticado;
import br.com.gerenciador.service.RegraNegocioException;
import br.com.gerenciador.service.UsuarioAdminService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;
import java.util.List;

/** CRUD de usuários. Só ADMIN acessa /admin/** (regra em SecurityConfig). */
@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioAdminController {

    private final UsuarioAdminService usuarioAdminService;
    private final SessoesUsuario sessoesUsuario;

    public UsuarioAdminController(UsuarioAdminService usuarioAdminService, SessoesUsuario sessoesUsuario) {
        this.usuarioAdminService = usuarioAdminService;
        this.sessoesUsuario = sessoesUsuario;
    }

    // parametrosInvalidos: um filtro inválido na URL (ex.: perfil=xyz) é só ignorado, em vez de dar erro.
    @GetMapping
    public String listar(@ModelAttribute("filtro") FiltroUsuarios filtro, BindingResult parametrosInvalidos,
                         @AuthenticationPrincipal UsuarioAutenticado admin, Model model) {
        model.addAttribute("pagina", usuarioAdminService.buscar(filtro.getQ(), filtro.getPerfil(), filtro.getPagina()));
        model.addAttribute("perfis", Perfil.values());
        model.addAttribute("agora", OffsetDateTime.now());
        model.addAttribute("idLogado", admin.getId());
        return "admin/usuarios/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("usuarioForm", new UsuarioForm());
        return formulario(model, null);
    }

    @PostMapping("/novo")
    public String criar(@Valid @ModelAttribute("usuarioForm") UsuarioForm form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                Usuario usuario = usuarioAdminService.criar(form);
                redirect.addFlashAttribute("sucesso", "Usuário “" + usuario.getLogin() + "” criado.");
                return "redirect:/admin/usuarios";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return formulario(model, null);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("usuarioForm", UsuarioForm.de(usuarioAdminService.buscar(id)));
        return formulario(model, id);
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("usuarioForm") UsuarioForm form,
                            BindingResult resultado, @AuthenticationPrincipal UsuarioAutenticado admin,
                            HttpSession sessao, Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                usuarioAdminService.atualizar(id, admin.getId(), form);
                // Perfil ou senha podem ter mudado: o usuário editado entra de novo (a sessão de quem editou continua).
                sessoesUsuario.encerrar(id, sessao.getId());
                redirect.addFlashAttribute("sucesso", "Usuário salvo.");
                return "redirect:/admin/usuarios";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return formulario(model, id);
    }

    @GetMapping("/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado admin,
                                    Model model, RedirectAttributes redirect) {
        if (id.equals(admin.getId())) {
            redirect.addFlashAttribute("erro", "Você não pode excluir a própria conta por aqui. Use a tela Meu perfil.");
            return "redirect:/admin/usuarios";
        }
        Usuario usuario = usuarioAdminService.buscar(id);
        long tarefas = usuarioAdminService.contarTarefas(id);
        long categorias = usuarioAdminService.contarCategorias(id);
        model.addAttribute("confirmacao", new ConfirmacaoExclusao("usuário", "usuarios", "Usuários",
                "/admin/usuarios", "/admin/usuarios/" + id + "/excluir",
                List.of(new Campo("Nome", usuario.getNome()),
                        new Campo("Usuário", usuario.getLogin()),
                        new Campo("Perfil", usuario.getPerfil().getDescricao())),
                "Também serão apagadas as " + tarefas + " tarefa(s) e as " + categorias + " categoria(s) deste usuário."));
        return "confirmar-exclusao";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado admin,
                          RedirectAttributes redirect) {
        try {
            usuarioAdminService.excluir(id, admin.getId());
            sessoesUsuario.encerrar(id, null);
            redirect.addFlashAttribute("sucesso", "Usuário excluído.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    private String formulario(Model model, Long usuarioId) {
        model.addAttribute("usuarioId", usuarioId);
        model.addAttribute("perfis", Perfil.values());
        if (usuarioId != null) {
            model.addAttribute("editado", usuarioAdminService.buscar(usuarioId));
            model.addAttribute("agora", OffsetDateTime.now());
        }
        return "admin/usuarios/form";
    }
}
