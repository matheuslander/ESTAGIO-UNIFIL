package br.com.uniaoacabamentos.repository;

import br.com.uniaoacabamentos.model.Material;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    @Query("select m from Material m where m.ativo = true or m.ativo is null")
    List<Material> findAllAtivos();

    @Query("select m from Material m where m.id = :id and (m.ativo = true or m.ativo is null)")
    Optional<Material> findAtivoById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Material m where m.id = :id and (m.ativo = true or m.ativo is null)")
    Optional<Material> findAtivoByIdForUpdate(@Param("id") Long id);

    @Query("select m from Material m where m.ativo = false")
    List<Material> findAllInativos();

    @Query("select m from Material m where m.id = :id and m.ativo = false")
    Optional<Material> findInativoById(@Param("id") Long id);
}
