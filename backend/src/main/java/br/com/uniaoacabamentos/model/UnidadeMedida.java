package br.com.uniaoacabamentos.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum UnidadeMedida {
    MM(new BigDecimal("1000")),
    CM(new BigDecimal("100")),
    M(BigDecimal.ONE);

    private final BigDecimal divisorParaMetros;

    UnidadeMedida(BigDecimal divisorParaMetros) {
        this.divisorParaMetros = divisorParaMetros;
    }

    public BigDecimal paraMetros(BigDecimal valor) {
        return valor.divide(divisorParaMetros, 12, RoundingMode.HALF_UP);
    }
}
