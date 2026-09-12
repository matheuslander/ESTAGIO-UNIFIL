package br.com.uniaoacabamentos.repository;

import br.com.uniaoacabamentos.model.Orcamento;
import br.com.uniaoacabamentos.model.StatusOrcamento;
import br.com.uniaoacabamentos.model.TipoOrcamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {
    @EntityGraph(attributePaths = {"usuarioCriador", "material", "obra", "obraMontador", "itens", "itens.material"})
    List<Orcamento> findAllByOrderByDataCriacaoDesc();

    @EntityGraph(attributePaths = {"usuarioCriador", "material", "obra", "obraMontador", "itens", "itens.material"})
    List<Orcamento> findByUsuarioCriador_IdOrderByDataCriacaoDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuarioCriador", "material", "obra", "obraMontador", "itens", "itens.material"})
    List<Orcamento> findByTipoOrcamentoOrderByDataCriacaoDesc(TipoOrcamento tipo);

    @EntityGraph(attributePaths = {"usuarioCriador", "material", "obra", "obraMontador", "itens", "itens.material"})
    List<Orcamento> findByTipoOrcamentoAndUsuarioCriador_IdOrderByDataCriacaoDesc(
            TipoOrcamento tipo, Long usuarioId);

    @EntityGraph(attributePaths = {"usuarioCriador", "material", "obra", "obraMontador", "itens", "itens.material"})
    List<Orcamento> findByTipoOrcamentoAndStatusAndObraIsNullOrderByDataCriacaoDesc(
            TipoOrcamento tipo, StatusOrcamento status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"usuarioCriador", "material", "obra", "obraMontador", "itens", "itens.material"})
    @Query("select o from Orcamento o where o.id = :id")
    Optional<Orcamento> findByIdForUpdate(@Param("id") Long id);

}
