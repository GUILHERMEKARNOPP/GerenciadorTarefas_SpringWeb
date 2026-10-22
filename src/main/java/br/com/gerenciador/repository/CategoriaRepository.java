package br.com.gerenciador.repository;

import br.com.gerenciador.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Toda consulta recebe o id do dono: um usuário nunca enxerga a categoria de outro. */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    @Query("select c from Categoria c where c.usuario.id = :usuarioId order by lower(c.nome)")
    List<Categoria> listarDoUsuario(@Param("usuarioId") Long usuarioId);

    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    @Query("""
            select count(c) > 0 from Categoria c
            where c.usuario.id = :usuarioId and lower(c.nome) = lower(:nome)
              and (:ignorarId is null or c.id <> :ignorarId)
            """)
    boolean existeNome(@Param("usuarioId") Long usuarioId, @Param("nome") String nome, @Param("ignorarId") Long ignorarId);
}
