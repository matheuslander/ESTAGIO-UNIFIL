package br.com.uniaoacabamentos.repository;

import br.com.uniaoacabamentos.model.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
    long countByMaterialId(Long materialId);

    long countByObraId(Long obraId);
}
