package br.com.uniaoacabamentos.dto;

import br.com.uniaoacabamentos.model.StatusObra;

import java.time.LocalDateTime;

public record ObraRequest(
        Long orcamentoId,
        String nomeObra,
        String endereco,
        String descricao,
        LocalDateTime dataInicio,
        StatusObra status
) {
}
