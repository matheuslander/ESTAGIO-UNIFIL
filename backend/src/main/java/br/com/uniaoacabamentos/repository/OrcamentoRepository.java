package br.com.uniaoacabamentos.repository;

import br.com.uniaoacabamentos.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {
    long countByObraId(Long obraId);

    long countByUsuarioId(Long usuarioId);
}
