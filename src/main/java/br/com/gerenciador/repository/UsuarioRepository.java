package br.com.gerenciador.repository;

import br.com.gerenciador.model.Perfil;
import br.com.gerenciador.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // lower(...) = lower(...) para usar o índice único uq_usuario_login.
    @Query("select u from Usuario u where lower(u.login) = lower(:login)")
    Optional<Usuario> buscarPorLogin(@Param("login") String login);

    @Query("""
            select count(u) > 0 from Usuario u
            where lower(u.login) = lower(:login) and (:ignorarId is null or u.id <> :ignorarId)
            """)
    boolean existeLogin(@Param("login") String login, @Param("ignorarId") Long ignorarId);

    @Query("""
            select count(u) > 0 from Usuario u
            where lower(u.email) = lower(:email) and (:ignorarId is null or u.id <> :ignorarId)
            """)
    boolean existeEmail(@Param("email") String email, @Param("ignorarId") Long ignorarId);

    long countByPerfil(Perfil perfil);

    /** @param texto padrão LIKE já em minúsculas e com curingas escapados por '!', ou null para não filtrar. */
    @Query("""
            select u from Usuario u
            where (:perfil is null or u.perfil = :perfil)
              and (:texto is null
                   or lower(u.nome) like :texto escape '!'
                   or lower(u.login) like :texto escape '!'
                   or lower(u.email) like :texto escape '!')
            """)
    Page<Usuario> buscar(@Param("texto") String texto, @Param("perfil") Perfil perfil, Pageable pageable);

    /** Ao atingir o limite, bloqueia o login e zera o contador para a próxima rodada (atômico no banco). */
    @Modifying
    @Query(value = """
            UPDATE usuario SET
                tentativas_falhas = CASE WHEN tentativas_falhas + 1 >= :maximo THEN 0 ELSE tentativas_falhas + 1 END,
                bloqueado_ate     = CASE WHEN tentativas_falhas + 1 >= :maximo
                                         THEN now() + :segundos * interval '1 second' ELSE bloqueado_ate END
            WHERE lower(login) = lower(:login)
            """, nativeQuery = true)
    void registrarFalhaLogin(@Param("login") String login, @Param("maximo") int maximo, @Param("segundos") int segundos);

    @Modifying
    @Query("update Usuario u set u.tentativasFalhas = 0, u.bloqueadoAte = null where u.id = :id")
    void registrarLoginComSucesso(@Param("id") Long id);
}
