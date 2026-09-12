package br.com.uniaoacabamentos.repository;

import br.com.uniaoacabamentos.model.ItemOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Long> {
    List<ItemOrcamento> findByOrcamento_Id(Long orcamentoId);
}
