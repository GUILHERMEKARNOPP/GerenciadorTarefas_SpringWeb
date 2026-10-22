package br.com.gerenciador.controller;

import br.com.gerenciador.controller.admin.StatusController;
import br.com.gerenciador.model.Perfil;
import br.com.gerenciador.model.Usuario;
import br.com.gerenciador.security.SecurityConfig;
import br.com.gerenciador.security.UsuarioAutenticado;
import br.com.gerenciador.service.CategoriaService;
import br.com.gerenciador.service.PrioridadeService;
import br.com.gerenciador.service.StatusService;
import br.com.gerenciador.service.TarefaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regras de acesso conferidas no servidor: login obrigatório, área admin, CSRF e dono vindo da sessão. */
@WebMvcTest(controllers = {TarefaController.class, StatusController.class})
@Import({SecurityConfig.class, FormatoData.class, Paginacao.class})
class SegurancaWebTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private TarefaService tarefaService;
    @MockitoBean
    private CategoriaService categoriaService;
    @MockitoBean
    private StatusService statusService;
    @MockitoBean
    private PrioridadeService prioridadeService;

    @Test
    void visitanteEhMandadoParaOLogin() throws Exception {
        mvc.perform(get("/tarefas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void usuarioComumNaoEntraNaAreaAdmin() throws Exception {
        mvc.perform(get("/admin/status").with(user(logado(42L, Perfil.USUARIO))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(statusService);
    }

    @Test
    void administradorEntraNaAreaAdmin() throws Exception {
        when(statusService.listarComTotais()).thenReturn(List.of());

        mvc.perform(get("/admin/status").with(user(logado(1L, Perfil.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("default-src 'self'")));
    }

    @Test
    void postSemTokenCsrfEhRecusado() throws Exception {
        mvc.perform(post("/tarefas/5/excluir").with(user(logado(42L, Perfil.USUARIO))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(tarefaService);
    }

    @Test
    void excluirUsaOIdDoUsuarioLogado() throws Exception {
        mvc.perform(post("/tarefas/5/excluir").with(user(logado(42L, Perfil.USUARIO))).with(csrf()))
                .andExpect(redirectedUrl("/tarefas"));
        verify(tarefaService).excluir(5L, 42L);
    }

    private static UsuarioAutenticado logado(Long id, Perfil perfil) {
        Usuario usuario = new Usuario("Nome", "nome@example.com", "nome", "{noop}x", perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        return new UsuarioAutenticado(usuario, OffsetDateTime.now());
    }
}
