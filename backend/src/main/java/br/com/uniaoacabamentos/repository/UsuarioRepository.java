package br.com.uniaoacabamentos.repository;

import br.com.uniaoacabamentos.model.StatusUsuario;
import br.com.uniaoacabamentos.model.TipoUsuario;
import br.com.uniaoacabamentos.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByLoginIgnoreCase(String login);
    boolean existsByLoginIgnoreCase(String login);
    boolean existsByLoginIgnoreCaseAndIdNot(String login, Long id);

    @Query("""
            select u from Usuario u
            where u.status = :ativo or (u.status is null and (u.ativo = true or u.ativo is null))
            order by u.nome
            """)
    List<Usuario> findAtivos(@Param("ativo") StatusUsuario ativo);

    @Query("""
            select u from Usuario u
            where u.status = :inativo or (u.status is null and u.ativo = false)
            order by u.nome
            """)
    List<Usuario> findInativos(@Param("inativo") StatusUsuario inativo);

    @Query("""
            select u from Usuario u where u.id = :id
              and (u.status = :ativo or (u.status is null and (u.ativo = true or u.ativo is null)))
            """)
    Optional<Usuario> findAtivoById(@Param("id") Long id,
                                    @Param("ativo") StatusUsuario ativo);

    @Query("""
            select u from Usuario u where u.id = :id
              and (u.status = :inativo or (u.status is null and u.ativo = false))
            """)
    Optional<Usuario> findInativoById(@Param("id") Long id,
                                      @Param("inativo") StatusUsuario inativo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select u from Usuario u
            where u.tipoUsuario = :tipo
              and (u.status = :ativo or (u.status is null and (u.ativo = true or u.ativo is null)))
            """)
    List<Usuario> bloquearAdministradoresAtivos(@Param("tipo") TipoUsuario tipo,
                                                 @Param("ativo") StatusUsuario ativo);
}
