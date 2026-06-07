package br.com.uniaoacabamentos.dto;

import br.com.uniaoacabamentos.model.TipoMovimentacao;

public record MovimentacaoRequest(Long materialId, Long usuarioId, Long obraId, TipoMovimentacao tipo,
                                  Integer quantidade, String observacao) {
}
