package br.com.uniaoacabamentos.dto;

import br.com.uniaoacabamentos.model.MovimentacaoEstoque;
import br.com.uniaoacabamentos.model.StatusObra;
import br.com.uniaoacabamentos.model.TipoMovimentacao;

import java.time.LocalDateTime;

public record MovimentacaoResponse(
        Long id,
        TipoMovimentacao tipo,
        Integer quantidade,
        LocalDateTime dataMovimentacao,
        LocalDateTime dataUltimaAlteracao,
        String observacao,
        Long materialId,
        String materialNome,
        Long usuarioId,
        String usuarioNome,
        Long obraId,
        String obraNome,
        StatusObra obraStatus
) {
    public static MovimentacaoResponse from(MovimentacaoEstoque movimentacao) {
        return new MovimentacaoResponse(
                movimentacao.getId(),
                movimentacao.getTipo(),
                movimentacao.getQuantidade(),
                movimentacao.getDataMovimentacao(),
                movimentacao.getDataUltimaAlteracao(),
                movimentacao.getObservacao(),
                movimentacao.getMaterial().getId(),
                movimentacao.getMaterial().getNome(),
                movimentacao.getUsuario().getId(),
                movimentacao.getUsuario().getNome(),
                movimentacao.getObra() == null ? null : movimentacao.getObra().getId(),
                movimentacao.getObra() == null ? null : movimentacao.getObra().getNomeObra(),
                movimentacao.getObra() == null ? null : movimentacao.getObra().getStatus()
        );
    }
}
