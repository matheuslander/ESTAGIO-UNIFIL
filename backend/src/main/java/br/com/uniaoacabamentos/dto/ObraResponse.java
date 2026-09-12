package br.com.uniaoacabamentos.dto;

import br.com.uniaoacabamentos.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ObraResponse(
        Long id,
        String nomeObra,
        String nomeCliente,
        String endereco,
        String descricao,
        Double metragem,
        BigDecimal metragemM2,
        BigDecimal valorContratado,
        String nomeMontador,
        BigDecimal valorMontador,
        StatusObra status,
        Boolean ativo,
        LocalDateTime dataInicio,
        LocalDateTime dataFinalizacao,
        LocalDateTime dataCriacao,
        OrcamentoOrigem orcamentoOrigem,
        OrcamentoMontadorResumo orcamentoMontador
) {
    public static ObraResponse from(Obra obra) {
        return new ObraResponse(
                obra.getId(), obra.getNomeObra(), obra.getNomeCliente(), obra.getEndereco(),
                obra.getDescricao(), obra.getMetragem(), obra.getMetragemM2(),
                obra.getValorContratado(), obra.getNomeMontador(), obra.getValorMontador(),
                obra.getStatus(), obra.getAtivo(), obra.getDataInicio(), obra.getDataFinalizacao(),
                obra.getDataCriacao(), OrcamentoOrigem.from(obra.getOrcamento()),
                OrcamentoMontadorResumo.from(obra.getOrcamentoMontador())
        );
    }

    public record OrcamentoOrigem(
            Long id,
            String nomeCliente,
            String nomeMaterial,
            BigDecimal larguraObraM,
            BigDecimal comprimentoObraM,
            BigDecimal areaObraM2,
            Integer quantidadePecas,
            BigDecimal valorTotal,
            StatusOrcamento status
    ) {
        public static OrcamentoOrigem from(Orcamento orcamento) {
            if (orcamento == null) return null;
            return new OrcamentoOrigem(
                    orcamento.getId(), orcamento.getNomeCliente(), orcamento.getNomeMaterial(),
                    orcamento.getLarguraObraM(), orcamento.getComprimentoObraM(),
                    orcamento.getAreaObraM2(), orcamento.getQuantidadePecas(),
                    orcamento.getValorTotal(), orcamento.getStatus()
            );
        }
    }

    public record OrcamentoMontadorResumo(
            Long id,
            String nomeMontador,
            BigDecimal metragemReferenciaM2,
            BigDecimal valorPorMetroQuadrado,
            BigDecimal valorTotal,
            StatusOrcamento status
    ) {
        public static OrcamentoMontadorResumo from(Orcamento orcamento) {
            if (orcamento == null) return null;
            return new OrcamentoMontadorResumo(orcamento.getId(), orcamento.getNomeMontador(),
                    orcamento.getMetragemReferenciaM2(), orcamento.getValorPorMetroQuadrado(),
                    orcamento.getValorTotal(), orcamento.getStatus());
        }
    }
}
