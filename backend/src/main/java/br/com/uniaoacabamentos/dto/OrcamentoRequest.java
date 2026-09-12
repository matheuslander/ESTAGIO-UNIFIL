package br.com.uniaoacabamentos.dto;

import java.math.BigDecimal;

public record OrcamentoRequest(
        String nomeCliente,
        BigDecimal larguraObraM,
        BigDecimal comprimentoObraM,
        Long materialId
) {
}
