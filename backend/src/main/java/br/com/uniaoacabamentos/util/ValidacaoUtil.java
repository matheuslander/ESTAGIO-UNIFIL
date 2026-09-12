package br.com.uniaoacabamentos.util;

import java.math.BigDecimal;

public final class ValidacaoUtil {
    private ValidacaoUtil() {
    }

    public static void exigirTextoComLetra(String valor, String nomeCampo) {
        exigirTextoComLetra(valor, nomeCampo, false);
    }

    public static void exigirTextoComLetra(String valor, String nomeCampo, boolean obrigatorio) {
        String texto = valor == null ? "" : valor.trim();

        if (obrigatorio && texto.isBlank()) {
            throw new RuntimeException(nomeCampo + " é obrigatório.");
        }

        if (!texto.isBlank() && !texto.matches(".*\\p{L}.*")) {
            throw new RuntimeException(nomeCampo + " precisa conter pelo menos uma letra; não pode ser apenas número.");
        }
    }

    public static void exigirInteiroNaoNegativo(Integer valor, String nomeCampo) {
        if (valor != null && valor < 0) {
            throw new RuntimeException(nomeCampo + " não pode ser negativo.");
        }
    }

    public static void exigirDoubleNaoNegativo(Double valor, String nomeCampo) {
        if (valor != null && valor < 0) {
            throw new RuntimeException(nomeCampo + " não pode ser negativo.");
        }
    }

    public static void exigirDecimalNaoNegativo(BigDecimal valor, String nomeCampo) {
        if (valor != null && valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(nomeCampo + " não pode ser negativo.");
        }
    }
}
