package br.com.uniaoacabamentos.dto;

import br.com.uniaoacabamentos.model.TipoOrcamento;

import java.math.BigDecimal;
import java.util.List;

public record OrcamentoRequest(
        Long usuarioId,
        Long obraId,
        TipoOrcamento tipoOrcamento,
        Double metragem,
        BigDecimal valorMaoObra,
        String observacao,
        List<OrcamentoItemRequest> itens
) {
}
