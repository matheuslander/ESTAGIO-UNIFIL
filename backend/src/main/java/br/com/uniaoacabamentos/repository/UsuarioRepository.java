package br.com.uniaoacabamentos.repository;

import br.com.uniaoacabamentos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByLoginAndIdNot(String login, Long id);
}
