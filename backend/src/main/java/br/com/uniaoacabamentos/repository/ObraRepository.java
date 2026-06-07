package br.com.uniaoacabamentos.repository;

import br.com.uniaoacabamentos.model.Obra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObraRepository extends JpaRepository<Obra, Long> {
    boolean existsByNomeObraIgnoreCaseAndNomeClienteIgnoreCaseAndEnderecoIgnoreCase(String nomeObra, String nomeCliente, String endereco);

    boolean existsByNomeObraIgnoreCaseAndNomeClienteIgnoreCaseAndEnderecoIgnoreCaseAndIdNot(String nomeObra, String nomeCliente, String endereco, Long id);
}
