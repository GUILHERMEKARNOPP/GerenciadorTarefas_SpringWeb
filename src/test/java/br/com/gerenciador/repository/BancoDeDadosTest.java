package br.com.gerenciador.repository;

import br.com.gerenciador.TestcontainersConfiguration;
import br.com.gerenciador.model.Categoria;
import br.com.gerenciador.model.Perfil;
import br.com.gerenciador.model.Prioridade;
import br.com.gerenciador.model.Status;
import br.com.gerenciador.model.Tarefa;
import br.com.gerenciador.model.Usuario;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Consultas e regras do banco (migrações do Flyway, chaves estrangeiras, índices únicos) num PostgreSQL de
 * verdade, criado pelo Testcontainers. Sem Docker rodando, estes testes são pulados.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class BancoDeDadosTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);

    @Autowired
    private TarefaRepository tarefaRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private PrioridadeRepository prioridadeRepository;
    @Autowired
    private EntityManager entityManager;

    private Usuario ana;
    private Usuario bruno;
    private Status pendente;
    private Prioridade alta;

    @BeforeEach
    void criarUsuarios() {
        ana = usuarioRepository.save(new Usuario("Ana", "ana@example.com", "ana", "{noop}x", Perfil.ADMIN));
        bruno = usuarioRepository.save(new Usuario("Bruno", "bruno@example.com", "bruno", "{noop}x", Perfil.USUARIO));
        pendente = statusRepository.findAllByOrderByIdAsc().getFirst();
        alta = prioridadeRepository.findAllByOrderByNivelAsc().getFirst();
    }

    @Test
    void buscaSoTrazTarefasDoDonoEAplicaOsFiltros() {
        Categoria faculdade = categoria(ana, "Faculdade");
        tarefa(ana, "Prova 50% estudada", faculdade, HOJE.minusDays(2));
        tarefa(ana, "Mercado", null, null);
        tarefa(bruno, "Prova do Bruno", null, null);
        PageRequest pagina = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("prioridade.nivel"), Sort.Order.asc("titulo")));

        assertThat(tarefaRepository.buscar(ana.getId(), null, null, null, null, pagina).getTotalElements()).isEqualTo(2);
        assertThat(tarefaRepository.buscar(ana.getId(), null, null, null, "%50!%%", pagina).getContent())
                .extracting(Tarefa::getTitulo).containsExactly("Prova 50% estudada");
        assertThat(tarefaRepository.buscar(ana.getId(), null, null, 0L, null, pagina).getContent())
                .extracting(Tarefa::getTitulo).containsExactly("Mercado");
        assertThat(tarefaRepository.buscar(ana.getId(), pendente.getId(), alta.getId(), faculdade.getId(), "%prova%", pagina)
                .getTotalElements()).isEqualTo(1);
        assertThat(tarefaRepository.contarAtrasadas(ana.getId(), HOJE)).isEqualTo(1);
    }

    @Test
    void bancoRecusaCategoriaDeOutroUsuarioNaTarefa() {
        Categoria doBruno = categoria(bruno, "Pessoal");

        assertThatThrownBy(() -> tarefa(ana, "Invasão", doBruno, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void excluirCategoriaDeixaATarefaSemCategoria() {
        Categoria faculdade = categoria(ana, "Faculdade");
        Tarefa tarefa = tarefa(ana, "Trabalho de BD", faculdade, null);
        // Como numa requisição nova: só a categoria é carregada antes de excluir (igual ao CategoriaService).
        entityManager.clear();

        categoriaRepository.delete(categoriaRepository.findById(faculdade.getId()).orElseThrow());
        categoriaRepository.flush();
        entityManager.clear();

        assertThat(tarefaRepository.findById(tarefa.getId())).get()
                .extracting(Tarefa::getCategoria).isNull();
    }

    @Test
    void mesmoUsuarioNaoRepeteNomeDeCategoria() {
        categoria(ana, "Faculdade");
        categoria(bruno, "faculdade");

        assertThatThrownBy(() -> categoria(ana, "FACULDADE")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void soUmStatusPodeFinalizarATarefa() {
        assertThatThrownBy(() -> statusRepository.saveAndFlush(new Status("Arquivada", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void quintaSenhaErradaBloqueiaOLoginEZeraAContagem() {
        for (int tentativa = 1; tentativa <= Usuario.MAX_TENTATIVAS_LOGIN; tentativa++) {
            usuarioRepository.registrarFalhaLogin("ANA", Usuario.MAX_TENTATIVAS_LOGIN, 300);
        }
        entityManager.clear();

        Usuario bloqueada = usuarioRepository.findById(ana.getId()).orElseThrow();
        assertThat(bloqueada.estaBloqueado(OffsetDateTime.now())).isTrue();
        assertThat(bloqueada.getTentativasFalhas()).isZero();
    }

    private Categoria categoria(Usuario dono, String nome) {
        Categoria categoria = new Categoria(dono);
        categoria.setNome(nome);
        return categoriaRepository.saveAndFlush(categoria);
    }

    private Tarefa tarefa(Usuario dono, String titulo, Categoria categoria, LocalDate vencimento) {
        Tarefa tarefa = new Tarefa(dono);
        tarefa.setTitulo(titulo);
        tarefa.setCategoria(categoria);
        tarefa.setStatus(pendente);
        tarefa.setPrioridade(alta);
        tarefa.setDataVencimento(vencimento);
        return tarefaRepository.saveAndFlush(tarefa);
    }
}
