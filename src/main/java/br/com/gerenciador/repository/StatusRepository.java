package br.com.gerenciador.repository;

import br.com.gerenciador.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StatusRepository extends JpaRepository<Status, Long> {

    List<Status> findAllByOrderByIdAsc();

    Optional<Status> findByFinalizaTrue();

    @Query("""
            select count(s) > 0 from Status s
            where lower(s.nome) = lower(:nome) and (:ignorarId is null or s.id <> :ignorarId)
            """)
    boolean existeNome(@Param("nome") String nome, @Param("ignorarId") Long ignorarId);

    @Modifying
    @Query("update Status s set s.finaliza = false where s.finaliza = true")
    void desmarcarFinaliza();
}
