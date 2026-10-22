package br.com.gerenciador.service;

import br.com.gerenciador.dto.UsuarioForm;
import br.com.gerenciador.model.Perfil;
import br.com.gerenciador.model.Usuario;
import br.com.gerenciador.repository.CategoriaRepository;
import br.com.gerenciador.repository.TarefaRepository;
import br.com.gerenciador.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioAdminServiceTest {

    private static final Long ADMIN_LOGADO = 1L;

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TarefaRepository tarefaRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioAdminService service;

    @BeforeEach
    void criarServico() {
        service = new UsuarioAdminService(usuarioRepository, tarefaRepository, categoriaRepository,
                new CadastroUsuarios(usuarioRepository), passwordEncoder);
    }

    @Test
    void adminNaoExcluiAPropriaConta() {
        assertThatThrownBy(() -> service.excluir(ADMIN_LOGADO, ADMIN_LOGADO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Meu perfil");
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void adminNaoTiraOProprioPerfilDeAdministrador() {
        when(usuarioRepository.findById(ADMIN_LOGADO)).thenReturn(Optional.of(usuario(ADMIN_LOGADO, Perfil.ADMIN)));

        assertThatThrownBy(() -> service.atualizar(ADMIN_LOGADO, ADMIN_LOGADO, form(Perfil.USUARIO)))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        erro -> assertThat(erro.getCampo()).isEqualTo("perfil"));
    }

    @Test
    void ultimoAdministradorNaoPodeSerExcluido() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, Perfil.ADMIN)));
        when(usuarioRepository.countByPerfil(Perfil.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.excluir(2L, ADMIN_LOGADO)).isInstanceOf(RegraNegocioException.class);
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void novoUsuarioPrecisaDeSenha() {
        assertThatThrownBy(() -> service.criar(form(Perfil.USUARIO)))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        erro -> assertThat(erro.getCampo()).isEqualTo("senha"));
    }

    private static Usuario usuario(Long id, Perfil perfil) {
        Usuario usuario = new Usuario("Nome", "nome@example.com", "nome", "hash", perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private static UsuarioForm form(Perfil perfil) {
        UsuarioForm form = new UsuarioForm();
        form.setNome("Nome");
        form.setEmail("nome@example.com");
        form.setLogin("nome");
        form.setPerfil(perfil);
        return form;
    }
}
