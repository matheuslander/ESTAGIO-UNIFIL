package br.com.uniaoacabamentos.dto;

import java.math.BigDecimal;

public record OrcamentoMontadorRequest(
        Long obraId,
        String nomeMontador,
        BigDecimal valorPorMetroQuadrado
) {
}
