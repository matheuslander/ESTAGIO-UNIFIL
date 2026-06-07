package br.com.uniaoacabamentos.repository;

import br.com.uniaoacabamentos.model.ItemOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Long> {
    long countByMaterialId(Long materialId);
}
