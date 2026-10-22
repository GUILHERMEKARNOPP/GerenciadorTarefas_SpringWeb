package br.com.gerenciador.controller;

import br.com.gerenciador.controller.ConfirmacaoExclusao.Campo;
import br.com.gerenciador.dto.CategoriaForm;
import br.com.gerenciador.model.Categoria;
import br.com.gerenciador.security.UsuarioAutenticado;
import br.com.gerenciador.service.CategoriaService;
import br.com.gerenciador.service.RegraNegocioException;
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

import java.util.List;

/** CRUD das categorias do usuário logado. */
@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String listar(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("categorias", categoriaService.listarComTotais(usuario.getId()));
        return "categorias/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("categoria", new CategoriaForm());
        return formulario(model, null);
    }

    @PostMapping("/nova")
    public String criar(@Valid @ModelAttribute("categoria") CategoriaForm form, BindingResult resultado,
                        @AuthenticationPrincipal UsuarioAutenticado usuario, Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                Categoria categoria = categoriaService.criar(usuario.getId(), form);
                redirect.addFlashAttribute("sucesso", "Categoria “" + categoria.getNome() + "” criada.");
                return "redirect:/categorias";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return formulario(model, null);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("categoria", CategoriaForm.de(categoriaService.buscarDoUsuario(id, usuario.getId())));
        return formulario(model, id);
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("categoria") CategoriaForm form,
                            BindingResult resultado, @AuthenticationPrincipal UsuarioAutenticado usuario,
                            Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                categoriaService.atualizar(id, usuario.getId(), form);
                redirect.addFlashAttribute("sucesso", "Categoria salva.");
                return "redirect:/categorias";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return formulario(model, id);
    }

    @GetMapping("/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
                                    Model model) {
        Categoria categoria = categoriaService.buscarDoUsuario(id, usuario.getId());
        long tarefas = categoriaService.contarTarefas(id, usuario.getId());
        String aviso = tarefas == 0 ? null : (tarefas == 1
                ? "A tarefa desta categoria não será apagada: ela fica como “Sem categoria”."
                : "As " + tarefas + " tarefas desta categoria não serão apagadas: elas ficam como “Sem categoria”.");
        model.addAttribute("confirmacao", new ConfirmacaoExclusao("categoria", "categorias", "Categorias",
                "/categorias", "/categorias/" + id + "/excluir",
                List.of(new Campo("Nome", categoria.getNome()),
                        new Campo("Descrição", categoria.getDescricao() == null ? "—" : categoria.getDescricao()),
                        new Campo("Tarefas", String.valueOf(tarefas))),
                aviso));
        return "confirmar-exclusao";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
                          RedirectAttributes redirect) {
        categoriaService.excluir(id, usuario.getId());
        redirect.addFlashAttribute("sucesso", "Categoria excluída.");
        return "redirect:/categorias";
    }

    private String formulario(Model model, Long categoriaId) {
        model.addAttribute("categoriaId", categoriaId);
        return "categorias/form";
    }
}
