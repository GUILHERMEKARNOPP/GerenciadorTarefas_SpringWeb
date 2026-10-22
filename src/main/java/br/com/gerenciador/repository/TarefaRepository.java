package br.com.gerenciador.repository;

import br.com.gerenciador.model.Tarefa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Toda consulta de tarefa de um usuário recebe o id dele: ninguém acessa a tarefa de outra pessoa. */
public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    /** Filtros opcionais: null não filtra. categoriaId = 0 busca as tarefas sem categoria. */
    String FILTRO_TAREFAS = """
            where t.usuario.id = :usuarioId
              and (:statusId is null or t.status.id = :statusId)
              and (:prioridadeId is null or t.prioridade.id = :prioridadeId)
              and (:categoriaId is null
                   or (:categoriaId = 0 and t.categoria is null)
                   or c.id = :categoriaId)
              and (:texto is null
                   or lower(t.titulo) like :texto escape '!'
                   or lower(t.descricao) like :texto escape '!')
            """;

    // Os "join fetch" trazem categoria, status e prioridade na mesma consulta (evita N+1 na tela).
    @Query(value = """
            select t from Tarefa t
              left join fetch t.categoria c
              join fetch t.status
              join fetch t.prioridade
            """ + FILTRO_TAREFAS,
            countQuery = "select count(t) from Tarefa t left join t.categoria c " + FILTRO_TAREFAS)
    Page<Tarefa> buscar(@Param("usuarioId") Long usuarioId,
                        @Param("statusId") Long statusId,
                        @Param("prioridadeId") Long prioridadeId,
                        @Param("categoriaId") Long categoriaId,
                        @Param("texto") String texto,
                        Pageable pageable);

    @Query("""
            select t from Tarefa t
              left join fetch t.categoria
              join fetch t.status
              join fetch t.prioridade
            where t.id = :id and t.usuario.id = :usuarioId
            """)
    Optional<Tarefa> buscarDoUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query("""
            select new br.com.gerenciador.repository.ContagemPorId(t.status.id, count(t))
            from Tarefa t where t.usuario.id = :usuarioId group by t.status.id
            """)
    List<ContagemPorId> contarPorStatusDoUsuario(@Param("usuarioId") Long usuarioId);

    @Query("""
            select count(t) from Tarefa t
            where t.usuario.id = :usuarioId and t.dataVencimento < :hoje and t.status.finaliza = false
            """)
    long contarAtrasadas(@Param("usuarioId") Long usuarioId, @Param("hoje") LocalDate hoje);

    @Query("""
            select new br.com.gerenciador.repository.ContagemPorId(t.categoria.id, count(t))
            from Tarefa t where t.usuario.id = :usuarioId and t.categoria is not null group by t.categoria.id
            """)
    List<ContagemPorId> contarPorCategoria(@Param("usuarioId") Long usuarioId);

    @Query("select new br.com.gerenciador.repository.ContagemPorId(t.status.id, count(t)) from Tarefa t group by t.status.id")
    List<ContagemPorId> contarPorStatus();

    @Query("select new br.com.gerenciador.repository.ContagemPorId(t.prioridade.id, count(t)) from Tarefa t group by t.prioridade.id")
    List<ContagemPorId> contarPorPrioridade();

    @Query("""
            select new br.com.gerenciador.repository.ContagemPorId(t.usuario.id, count(t))
            from Tarefa t where t.usuario.id in :usuarioIds group by t.usuario.id
            """)
    List<ContagemPorId> contarPorUsuario(@Param("usuarioIds") Collection<Long> usuarioIds);

    long countByUsuarioId(Long usuarioId);

    long countByCategoriaId(Long categoriaId);

    long countByStatusId(Long statusId);

    long countByPrioridadeId(Long prioridadeId);
}
