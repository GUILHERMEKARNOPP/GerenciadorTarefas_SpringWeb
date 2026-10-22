package br.com.gerenciador.controller;

import br.com.gerenciador.controller.ConfirmacaoExclusao.Campo;
import br.com.gerenciador.dto.FiltroTarefas;
import br.com.gerenciador.dto.OrdemTarefas;
import br.com.gerenciador.dto.TarefaForm;
import br.com.gerenciador.model.Tarefa;
import br.com.gerenciador.security.UsuarioAutenticado;
import br.com.gerenciador.service.CategoriaService;
import br.com.gerenciador.service.PrioridadeService;
import br.com.gerenciador.service.RegraNegocioException;
import br.com.gerenciador.service.StatusService;
import br.com.gerenciador.service.TarefaService;
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

import java.time.LocalDate;
import java.util.List;

/** CRUD de tarefas. O dono vem sempre do usuário logado, nunca de um campo do formulário. */
@Controller
@RequestMapping("/tarefas")
public class TarefaController {

    private final TarefaService tarefaService;
    private final CategoriaService categoriaService;
    private final StatusService statusService;
    private final PrioridadeService prioridadeService;
    private final FormatoData formato;

    public TarefaController(TarefaService tarefaService, CategoriaService categoriaService,
                            StatusService statusService, PrioridadeService prioridadeService, FormatoData formato) {
        this.tarefaService = tarefaService;
        this.categoriaService = categoriaService;
        this.statusService = statusService;
        this.prioridadeService = prioridadeService;
        this.formato = formato;
    }

    // parametrosInvalidos: um filtro inválido na URL (ex.: status=abc) é só ignorado, em vez de dar erro.
    @GetMapping
    public String listar(@ModelAttribute("filtro") FiltroTarefas filtro, BindingResult parametrosInvalidos,
                         @AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        LocalDate hoje = LocalDate.now();
        model.addAttribute("pagina", tarefaService.buscar(usuario.getId(), filtro));
        model.addAttribute("resumo", tarefaService.resumir(usuario.getId(), hoje));
        model.addAttribute("hoje", hoje);
        model.addAttribute("ordens", OrdemTarefas.values());
        adicionarOpcoes(model, usuario.getId());
        return "tarefas/lista";
    }

    @GetMapping("/nova")
    public String nova(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        TarefaForm form = new TarefaForm();
        statusService.listar().stream()
                .filter(status -> !status.isFinaliza())
                .findFirst()
                .ifPresent(status -> form.setStatusId(status.getId()));
        model.addAttribute("tarefa", form);
        return formulario(model, usuario.getId(), null);
    }

    @PostMapping("/nova")
    public String criar(@Valid @ModelAttribute("tarefa") TarefaForm form, BindingResult resultado,
                        @AuthenticationPrincipal UsuarioAutenticado usuario, Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                tarefaService.criar(usuario.getId(), form);
                redirect.addFlashAttribute("sucesso", "Tarefa criada.");
                return "redirect:/tarefas";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return formulario(model, usuario.getId(), null);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        Tarefa tarefa = tarefaService.buscarDoUsuario(id, usuario.getId());
        model.addAttribute("tarefa", TarefaForm.de(tarefa));
        model.addAttribute("criadaEm", tarefa.getDataCriacao());
        return formulario(model, usuario.getId(), id);
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("tarefa") TarefaForm form,
                            BindingResult resultado, @AuthenticationPrincipal UsuarioAutenticado usuario,
                            Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                tarefaService.atualizar(id, usuario.getId(), form);
                redirect.addFlashAttribute("sucesso", "Tarefa salva.");
                return "redirect:/tarefas";
            } catch (RegraNegocioException e) {
                ErrosFormulario.registrar(resultado, e);
            }
        }
        return formulario(model, usuario.getId(), id);
    }

    /** Volta para a lista com os mesmos filtros e página em que o usuário estava. */
    @PostMapping("/{id}/concluir")
    public String concluir(@PathVariable Long id, @ModelAttribute FiltroTarefas filtro, BindingResult parametrosInvalidos,
                           @AuthenticationPrincipal UsuarioAutenticado usuario, RedirectAttributes redirect) {
        try {
            tarefaService.concluir(id, usuario.getId());
            redirect.addFlashAttribute("sucesso", "Tarefa concluída.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        filtro.comoParametros().forEach(redirect::addAttribute);
        return "redirect:/tarefas";
    }

    @GetMapping("/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
                                    Model model) {
        Tarefa tarefa = tarefaService.buscarDoUsuario(id, usuario.getId());
        model.addAttribute("confirmacao", new ConfirmacaoExclusao("tarefa", "tarefas", "Tarefas", "/tarefas",
                "/tarefas/" + id + "/excluir",
                List.of(new Campo("Título", tarefa.getTitulo()),
                        new Campo("Categoria", tarefa.getCategoria() == null ? "Sem categoria" : tarefa.getCategoria().getNome()),
                        new Campo("Prioridade", tarefa.getPrioridade().getNome()),
                        new Campo("Status", tarefa.getStatus().getNome()),
                        new Campo("Vencimento", tarefa.getDataVencimento() == null
                                ? "Sem prazo" : formato.data(tarefa.getDataVencimento()))),
                null));
        return "confirmar-exclusao";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario,
                          RedirectAttributes redirect) {
        tarefaService.excluir(id, usuario.getId());
        redirect.addFlashAttribute("sucesso", "Tarefa excluída.");
        return "redirect:/tarefas";
    }

    private String formulario(Model model, Long usuarioId, Long tarefaId) {
        model.addAttribute("tarefaId", tarefaId);
        adicionarOpcoes(model, usuarioId);
        return "tarefas/form";
    }

    private void adicionarOpcoes(Model model, Long usuarioId) {
        model.addAttribute("categorias", categoriaService.listar(usuarioId));
        model.addAttribute("statusOpcoes", statusService.listar());
        model.addAttribute("prioridades", prioridadeService.listar());
    }
}
