package br.com.gerenciador.repository;

import br.com.gerenciador.model.Prioridade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PrioridadeRepository extends JpaRepository<Prioridade, Long> {

    List<Prioridade> findAllByOrderByNivelAsc();

    @Query("""
            select count(p) > 0 from Prioridade p
            where lower(p.nome) = lower(:nome) and (:ignorarId is null or p.id <> :ignorarId)
            """)
    boolean existeNome(@Param("nome") String nome, @Param("ignorarId") Long ignorarId);

    @Query("""
            select count(p) > 0 from Prioridade p
            where p.nivel = :nivel and (:ignorarId is null or p.id <> :ignorarId)
            """)
    boolean existeNivel(@Param("nivel") int nivel, @Param("ignorarId") Long ignorarId);
}
