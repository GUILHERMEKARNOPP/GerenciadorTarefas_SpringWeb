package br.com.gerenciador.controller.admin;

import br.com.gerenciador.controller.ConfirmacaoExclusao;
import br.com.gerenciador.controller.ConfirmacaoExclusao.Campo;
import br.com.gerenciador.controller.ErrosFormulario;
import br.com.gerenciador.dto.PrioridadeForm;
import br.com.gerenciador.model.Prioridade;
import br.com.gerenciador.service.PrioridadeService;
import br.com.gerenciador.service.RegraNegocioException;
import jakarta.validation.Valid;
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

/** CRUD de prioridades. Só ADMIN acessa /admin/** (regra em SecurityConfig). */
@Controller
@RequestMapping("/admin/prioridades")
public class PrioridadeController {

    private final PrioridadeService prioridadeService;

    public PrioridadeController(PrioridadeService prioridadeService) {
        this.prioridadeService = prioridadeService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("linhas", prioridadeService.listarComTotais());
        return "admin/prioridades/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("prioridadeForm", new PrioridadeForm());
        return formulario(model, null);
    }

    @PostMapping("/nova")
    public String criar(@Valid @ModelAttribute("prioridadeForm") PrioridadeForm form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                Prioridade prioridade = prioridadeService.criar(form);
                redirect.addFlashAttribute("sucesso", "Prioridade “" + prioridade.getNome() + "” criada.");
                return "redirect:/admin/prioridades";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return formulario(model, null);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("prioridadeForm", PrioridadeForm.de(prioridadeService.buscar(id)));
        return formulario(model, id);
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("prioridadeForm") PrioridadeForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                prioridadeService.atualizar(id, form);
                redirect.addFlashAttribute("sucesso", "Prioridade salva.");
                return "redirect:/admin/prioridades";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return formulario(model, id);
    }

    @GetMapping("/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, Model model) {
        Prioridade prioridade = prioridadeService.buscar(id);
        long tarefas = prioridadeService.contarTarefas(id);
        String aviso = tarefas == 0 ? null
                : "Esta prioridade está em uso por " + tarefas + " tarefa(s). Se você confirmar, o sistema vai recusar a exclusão.";
        model.addAttribute("confirmacao", new ConfirmacaoExclusao("prioridade", "prioridades", "Prioridades",
                "/admin/prioridades", "/admin/prioridades/" + id + "/excluir",
                List.of(new Campo("Nível", String.valueOf(prioridade.getNivel())),
                        new Campo("Nome", prioridade.getNome()),
                        new Campo("Tarefas", String.valueOf(tarefas))),
                aviso));
        return "confirmar-exclusao";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            prioridadeService.excluir(id);
            redirect.addFlashAttribute("sucesso", "Prioridade excluída.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/prioridades";
    }

    private String formulario(Model model, Long prioridadeId) {
        model.addAttribute("prioridadeId", prioridadeId);
        return "admin/prioridades/form";
    }
}
