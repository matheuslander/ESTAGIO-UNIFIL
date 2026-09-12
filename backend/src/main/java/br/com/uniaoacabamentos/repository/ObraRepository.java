package br.com.uniaoacabamentos.repository;

import br.com.uniaoacabamentos.model.Obra;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ObraRepository extends JpaRepository<Obra, Long> {
    @EntityGraph(attributePaths = {"orcamento", "orcamentoMontador"})
    @Query("select o from Obra o where o.ativo = true or o.ativo is null")
    List<Obra> findAllAtivas();

    @EntityGraph(attributePaths = {"orcamento", "orcamentoMontador"})
    @Query("select o from Obra o where o.id = :id and (o.ativo = true or o.ativo is null)")
    Optional<Obra> findAtivaById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"orcamento", "orcamentoMontador"})
    @Query("select o from Obra o where o.id = :id and (o.ativo = true or o.ativo is null)")
    Optional<Obra> findAtivaByIdForUpdate(@Param("id") Long id);

    @Query("select o from Obra o where o.ativo = false")
    List<Obra> findAllInativas();

    @Query("select o from Obra o where o.id = :id and o.ativo = false")
    Optional<Obra> findInativaById(@Param("id") Long id);

    @Query("""
            select count(o) from Obra o
            where (:idAtual is null or o.id <> :idAtual)
              and lower(trim(o.nomeObra)) = lower(trim(:nomeObra))
              and lower(trim(o.nomeCliente)) = lower(trim(:nomeCliente))
              and lower(trim(coalesce(o.endereco, ''))) = lower(trim(:endereco))
            """)
    long contarDuplicadas(@Param("nomeObra") String nomeObra,
                          @Param("nomeCliente") String nomeCliente,
                          @Param("endereco") String endereco,
                          @Param("idAtual") Long idAtual);
}
