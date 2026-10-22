package br.com.gerenciador.controller.admin;

import br.com.gerenciador.controller.ConfirmacaoExclusao;
import br.com.gerenciador.controller.ConfirmacaoExclusao.Campo;
import br.com.gerenciador.controller.ErrosFormulario;
import br.com.gerenciador.dto.StatusForm;
import br.com.gerenciador.model.Status;
import br.com.gerenciador.service.RegraNegocioException;
import br.com.gerenciador.service.StatusService;
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

/** CRUD de status. Só ADMIN acessa /admin/** (regra em SecurityConfig). */
@Controller
@RequestMapping("/admin/status")
public class StatusController {

    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("linhas", statusService.listarComTotais());
        return "admin/status/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("statusForm", new StatusForm());
        return formulario(model, null);
    }

    @PostMapping("/novo")
    public String criar(@Valid @ModelAttribute("statusForm") StatusForm form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                Status status = statusService.criar(form);
                redirect.addFlashAttribute("sucesso", "Status “" + status.getNome() + "” criado.");
                return "redirect:/admin/status";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return formulario(model, null);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("statusForm", StatusForm.de(statusService.buscar(id)));
        return formulario(model, id);
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("statusForm") StatusForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                statusService.atualizar(id, form);
                redirect.addFlashAttribute("sucesso", "Status salvo.");
                return "redirect:/admin/status";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return formulario(model, id);
    }

    @GetMapping("/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, Model model) {
        Status status = statusService.buscar(id);
        long tarefas = statusService.contarTarefas(id);
        String aviso = null;
        if (status.isFinaliza()) {
            aviso = "Este é o status que finaliza as tarefas. Se você confirmar, o sistema vai recusar a exclusão.";
        } else if (tarefas > 0) {
            aviso = "Este status está em uso por " + tarefas + " tarefa(s). Se você confirmar, o sistema vai recusar a exclusão.";
        }
        model.addAttribute("confirmacao", new ConfirmacaoExclusao("status", "status", "Status",
                "/admin/status", "/admin/status/" + id + "/excluir",
                List.of(new Campo("Nome", status.getNome()),
                        new Campo("Finaliza a tarefa", status.isFinaliza() ? "Sim" : "Não"),
                        new Campo("Tarefas", String.valueOf(tarefas))),
                aviso));
        return "confirmar-exclusao";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            statusService.excluir(id);
            redirect.addFlashAttribute("sucesso", "Status excluído.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/status";
    }

    private String formulario(Model model, Long statusId) {
        model.addAttribute("statusId", statusId);
        return "admin/status/form";
    }
}
