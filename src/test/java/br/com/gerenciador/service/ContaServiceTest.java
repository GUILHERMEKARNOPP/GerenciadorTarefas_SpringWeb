package br.com.gerenciador.service;

import br.com.gerenciador.dto.CadastroForm;
import br.com.gerenciador.dto.SenhaForm;
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
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TarefaRepository tarefaRepository;
    @Mock
    private CategoriaRepository categoriaRepository;

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private ContaService contaService;

    @BeforeEach
    void criarServico() {
        contaService = servico("");
    }

    private ContaService servico(String loginAdministrador) {
        return new ContaService(usuarioRepository, tarefaRepository, categoriaRepository,
                new CadastroUsuarios(usuarioRepository), passwordEncoder, loginAdministrador);
    }

    @Test
    void primeiraContaViraAdministradoraEGuardaSoOHash() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(usuarioRepository.saveAndFlush(any())).thenAnswer(chamada -> chamada.getArgument(0));

        Usuario usuario = contaService.cadastrar(cadastro("ana"));

        assertThat(usuario.getPerfil()).isEqualTo(Perfil.ADMIN);
        assertThat(usuario.getSenhaHash()).startsWith("{bcrypt}").doesNotContain("Segura123");
    }

    @Test
    void demaisContasSaoDeUsuarioComum() {
        when(usuarioRepository.count()).thenReturn(1L);
        when(usuarioRepository.saveAndFlush(any())).thenAnswer(chamada -> chamada.getArgument(0));

        assertThat(contaService.cadastrar(cadastro("bruno")).getPerfil()).isEqualTo(Perfil.USUARIO);
    }

    @Test
    void comLoginAdministradorConfiguradoSoEleViraAdmin() {
        ContaService producao = servico("Guilherme");
        when(usuarioRepository.saveAndFlush(any())).thenAnswer(chamada -> chamada.getArgument(0));

        // Quem chega primeiro não ganha o sistema: o perfil depende só do login configurado.
        assertThat(producao.cadastrar(cadastro("intruso")).getPerfil()).isEqualTo(Perfil.USUARIO);
        assertThat(producao.cadastrar(cadastro("guilherme")).getPerfil()).isEqualTo(Perfil.ADMIN);
        verify(usuarioRepository, never()).count();
    }

    @Test
    void loginRepetidoEhRecusado() {
        when(usuarioRepository.existeLogin("ana", null)).thenReturn(true);

        assertThatThrownBy(() -> contaService.cadastrar(cadastro("ana")))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        erro -> assertThat(erro.getCampo()).isEqualTo("login"));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void senhaAtualErradaContaComoTentativaDeLogin() {
        Usuario ana = new Usuario("Ana", "ana@example.com", "ana", passwordEncoder.encode("Segura123"), Perfil.USUARIO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana));
        SenhaForm form = new SenhaForm();
        form.setSenhaAtual("errada000");
        form.setSenha("Nova7890");
        form.setConfirmacaoSenha("Nova7890");

        assertThatThrownBy(() -> contaService.alterarSenha(1L, form))
                .isInstanceOfSatisfying(RegraNegocioException.class,
                        erro -> assertThat(erro.getCampo()).isEqualTo("senhaAtual"));
        verify(usuarioRepository).registrarFalhaLogin("ana", Usuario.MAX_TENTATIVAS_LOGIN, 300);
        assertThat(passwordEncoder.matches("Segura123", ana.getSenhaHash())).isTrue();
    }

    @Test
    void unicoAdministradorNaoExcluiAPropriaConta() {
        Usuario ana = new Usuario("Ana", "ana@example.com", "ana", passwordEncoder.encode("Segura123"), Perfil.ADMIN);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana));
        when(usuarioRepository.countByPerfil(Perfil.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> contaService.excluirConta(1L, "Segura123"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("único administrador");
        verify(usuarioRepository, never()).delete(any());
        verify(usuarioRepository, never()).registrarFalhaLogin(anyString(), anyInt(), anyInt());
    }

    private static CadastroForm cadastro(String login) {
        CadastroForm form = new CadastroForm();
        form.setNome("Nome");
        form.setEmail(login + "@example.com");
        form.setLogin(login);
        form.setSenha("Segura123");
        form.setConfirmacaoSenha("Segura123");
        return form;
    }
}
