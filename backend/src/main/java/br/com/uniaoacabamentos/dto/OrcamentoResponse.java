package br.com.uniaoacabamentos.dto;

import br.com.uniaoacabamentos.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrcamentoResponse(
        Long id,
        TipoOrcamento tipoOrcamento,
        String nomeCliente,
        String nomeMontador,
        Long materialId,
        String nomeMaterial,
        BigDecimal larguraObraM,
        BigDecimal comprimentoObraM,
        BigDecimal areaObraM2,
        BigDecimal larguraPecaCm,
        BigDecimal comprimentoPecaCm,
        BigDecimal areaPecaM2,
        Integer quantidadePecas,
        BigDecimal valorUnitario,
        BigDecimal metragemReferenciaM2,
        BigDecimal valorPorMetroQuadrado,
        BigDecimal valorMateriais,
        BigDecimal valorMaoDeObra,
        BigDecimal valorTotal,
        StatusOrcamento status,
        LocalDateTime dataCriacao,
        UsuarioResumo usuarioCriador,
        Long obraId,
        String nomeObra,
        List<ItemResumo> itens
) {
    public static OrcamentoResponse from(Orcamento orcamento) {
        List<ItemResumo> itens = orcamento.getItens().stream().map(ItemResumo::from).toList();
        ItemResumo primeiro = itens.isEmpty() ? null : itens.getFirst();
        Obra obraVinculada = orcamento.getTipoOrcamento() == TipoOrcamento.MONTADOR
                ? orcamento.getObraMontador() : orcamento.getObra();

        return new OrcamentoResponse(
                orcamento.getId(), orcamento.getTipoOrcamento(), orcamento.getNomeCliente(),
                orcamento.getNomeMontador(),
                primeiro == null ? idMaterialLegado(orcamento) : primeiro.materialId(),
                primeiro == null ? orcamento.getNomeMaterial() : primeiro.nomeMaterial(),
                orcamento.getLarguraObraM(), orcamento.getComprimentoObraM(),
                orcamento.getAreaObraM2(),
                primeiro == null ? orcamento.getLarguraPecaCm() : converterParaCm(primeiro.larguraPeca(), primeiro.unidadeMedida()),
                primeiro == null ? orcamento.getComprimentoPecaCm() : converterParaCm(primeiro.comprimentoPeca(), primeiro.unidadeMedida()),
                primeiro == null ? orcamento.getAreaPecaM2() : primeiro.areaPecaM2(),
                primeiro == null ? orcamento.getQuantidadePecas() : primeiro.quantidadePecas(),
                primeiro == null ? orcamento.getValorUnitario() : primeiro.valorUnitario(),
                orcamento.getMetragemReferenciaM2(), orcamento.getValorPorMetroQuadrado(),
                orcamento.getValorMateriais(), orcamento.getValorMaoDeObra(),
                orcamento.getValorTotal(), orcamento.getStatus(), orcamento.getDataCriacao(),
                UsuarioResumo.from(orcamento.getUsuarioCriador()),
                obraVinculada == null ? null : obraVinculada.getId(),
                obraVinculada == null ? null : obraVinculada.getNomeObra(), itens
        );
    }

    private static Long idMaterialLegado(Orcamento orcamento) {
        return orcamento.getMaterial() == null ? null : orcamento.getMaterial().getId();
    }

    private static BigDecimal converterParaCm(BigDecimal valor, UnidadeMedida unidade) {
        if (valor == null || unidade == null) return null;
        return switch (unidade) {
            case MM -> valor.divide(BigDecimal.TEN);
            case CM -> valor;
            case M -> valor.multiply(BigDecimal.valueOf(100));
        };
    }

    public record ItemResumo(
            Long id,
            Long materialId,
            String nomeMaterial,
            BigDecimal larguraPeca,
            BigDecimal comprimentoPeca,
            UnidadeMedida unidadeMedida,
            BigDecimal areaPecaM2,
            Integer quantidadePecas,
            BigDecimal valorUnitario,
            BigDecimal subtotal
    ) {
        public static ItemResumo from(ItemOrcamento item) {
            return new ItemResumo(item.getId(), item.getMaterial().getId(), item.getNomeMaterial(),
                    item.getLarguraPeca(), item.getComprimentoPeca(), item.getUnidadeMedida(),
                    item.getAreaPecaM2(), item.getQuantidadePecas(), item.getValorUnitario(),
                    item.getSubtotal());
        }
    }

    public record UsuarioResumo(Long id, String nome, String login, TipoUsuario tipoUsuario) {
        public static UsuarioResumo from(Usuario usuario) {
            if (usuario == null) return null;
            return new UsuarioResumo(usuario.getId(), usuario.getNome(), usuario.getLogin(),
                    usuario.getTipoUsuario().normalizado());
        }
    }
}
