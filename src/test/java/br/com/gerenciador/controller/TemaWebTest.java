package br.com.gerenciador.controller;

import br.com.gerenciador.model.Perfil;
import br.com.gerenciador.model.Usuario;
import br.com.gerenciador.security.SecurityConfig;
import br.com.gerenciador.security.UsuarioAutenticado;
import br.com.gerenciador.service.ContaService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Escolha do tema: só na primeira visita antes do login, guardada em cookie e restrita à lista de temas. */
@WebMvcTest(controllers = {TemaController.class, AcessoController.class})
@Import(SecurityConfig.class)
class TemaWebTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ContaService contaService;

    @Test
    void primeiraVisitaEscolheOTemaAntesDoLogin() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(redirectedUrl("/tema"));
    }

    @Test
    void quemJaEscolheuVaiDiretoParaOLogin() throws Exception {
        mvc.perform(get("/login").cookie(new Cookie("tema", "padrao")))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void cookieComTemaDesconhecidoContaComoPrimeiraVisita() throws Exception {
        mvc.perform(get("/login").cookie(new Cookie("tema", "inexistente")))
                .andExpect(redirectedUrl("/tema"));
    }

    @Test
    void primeiraVisitaNaoMarcaNenhumTemaComoAtual() throws Exception {
        mvc.perform(get("/tema"))
                .andExpect(status().isOk())
                .andExpect(view().name("tema"))
                .andExpect(content().string(not(containsString("Tema atual"))));
    }

    @Test
    void temaJaEscolhidoApareceComoAtual() throws Exception {
        mvc.perform(get("/tema").cookie(new Cookie("tema", "padrao")))
                .andExpect(content().string(containsString("Tema atual")));
    }

    @Test
    void escolherGuardaOTemaEmCookieEVaiParaOLogin() throws Exception {
        mvc.perform(post("/tema").param("tema", "padrao").with(csrf()))
                .andExpect(redirectedUrl("/login"))
                .andExpect(cookie().value("tema", "padrao"))
                .andExpect(cookie().httpOnly("tema", true))
                .andExpect(cookie().sameSite("tema", "Lax"));
    }

    @Test
    void usuarioLogadoTrocaOTemaEVoltaParaAsTarefas() throws Exception {
        mvc.perform(post("/tema").param("tema", "padrao").with(user(logado())).with(csrf()))
                .andExpect(redirectedUrl("/tarefas"))
                .andExpect(cookie().value("tema", "padrao"));
    }

    @Test
    void temaMarioCarregaOVisualEOsSonsEmTodasAsTelas() throws Exception {
        mvc.perform(get("/login").cookie(new Cookie("tema", "mario")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/temas/mario/tema.css")))
                .andExpect(content().string(containsString("/temas/mario/tema.js")));
    }

    @Test
    void temaPadraoNaoCarregaArquivosDeOutroTema() throws Exception {
        mvc.perform(get("/login").cookie(new Cookie("tema", "padrao")))
                .andExpect(content().string(not(containsString("tema.css"))))
                .andExpect(content().string(not(containsString("tema.js"))));
    }

    @Test
    void telaDeEscolhaMostraOMundoDeCadaTema() throws Exception {
        mvc.perform(get("/tema"))
                .andExpect(content().string(containsString("value=\"mario\"")))
                .andExpect(content().string(containsString("/temas/mario/mundo.css")))
                .andExpect(content().string(containsString("mario-encanador")))
                .andExpect(content().string(containsString("value=\"minecraft\"")))
                .andExpect(content().string(containsString("/temas/minecraft/mundo.css")))
                .andExpect(content().string(containsString("mc-minerador")))
                .andExpect(content().string(containsString("value=\"stardew\"")))
                .andExpect(content().string(containsString("/temas/stardew/mundo.css")))
                .andExpect(content().string(containsString("sv-abigail")))
                .andExpect(content().string(containsString("value=\"cs2\"")))
                .andExpect(content().string(containsString("/temas/cs2/mundo.css")))
                .andExpect(content().string(containsString("cs-arte")));
    }

    @Test
    void escolherMarioGuardaOTemaNoCookie() throws Exception {
        mvc.perform(post("/tema").param("tema", "mario").with(csrf()))
                .andExpect(redirectedUrl("/login"))
                .andExpect(cookie().value("tema", "mario"));
    }

    @Test
    void temaMinecraftCarregaSoOsArquivosDele() throws Exception {
        mvc.perform(get("/login").cookie(new Cookie("tema", "minecraft")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/temas/minecraft/tema.css")))
                .andExpect(content().string(containsString("/temas/minecraft/tema.js")))
                .andExpect(content().string(not(containsString("/temas/mario/"))));
    }

    @Test
    void escolherMinecraftGuardaOTemaNoCookie() throws Exception {
        mvc.perform(post("/tema").param("tema", "minecraft").with(csrf()))
                .andExpect(redirectedUrl("/login"))
                .andExpect(cookie().value("tema", "minecraft"));
    }

    @Test
    void temaStardewCarregaSoOsArquivosDele() throws Exception {
        mvc.perform(get("/login").cookie(new Cookie("tema", "stardew")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/temas/stardew/tema.css")))
                .andExpect(content().string(containsString("/temas/stardew/tema.js")))
                .andExpect(content().string(not(containsString("/temas/minecraft/"))));
    }

    @Test
    void escolherStardewGuardaOTemaNoCookie() throws Exception {
        mvc.perform(post("/tema").param("tema", "stardew").with(csrf()))
                .andExpect(redirectedUrl("/login"))
                .andExpect(cookie().value("tema", "stardew"));
    }

    @Test
    void temaCs2CarregaSoOsArquivosDele() throws Exception {
        mvc.perform(get("/login").cookie(new Cookie("tema", "cs2")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/temas/cs2/tema.css")))
                .andExpect(content().string(containsString("/temas/cs2/tema.js")))
                .andExpect(content().string(not(containsString("/temas/stardew/"))));
    }

    @Test
    void escolherCs2GuardaOTemaNoCookie() throws Exception {
        mvc.perform(post("/tema").param("tema", "cs2").with(csrf()))
                .andExpect(redirectedUrl("/login"))
                .andExpect(cookie().value("tema", "cs2"));
    }

    @Test
    void temaForaDaListaEhRecusado() throws Exception {
        mvc.perform(post("/tema").param("tema", "<script>").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(cookie().doesNotExist("tema"));
    }

    @Test
    void escolherSemTokenCsrfEhRecusado() throws Exception {
        mvc.perform(post("/tema").param("tema", "padrao"))
                .andExpect(status().isForbidden())
                .andExpect(cookie().doesNotExist("tema"));
    }

    private static UsuarioAutenticado logado() {
        Usuario usuario = new Usuario("Nome", "nome@example.com", "nome", "{noop}x", Perfil.USUARIO);
        ReflectionTestUtils.setField(usuario, "id", 42L);
        return new UsuarioAutenticado(usuario, OffsetDateTime.now());
    }
}
