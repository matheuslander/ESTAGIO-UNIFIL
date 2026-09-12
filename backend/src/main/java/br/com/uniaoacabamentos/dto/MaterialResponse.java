package br.com.uniaoacabamentos.dto;

import br.com.uniaoacabamentos.model.Material;
import br.com.uniaoacabamentos.model.UnidadeMedida;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MaterialResponse(
        Long id,
        String nome,
        String descricao,
        String marca,
        String cor,
        Integer quantidade,
        Integer estoqueMinimo,
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal valorCusto,
        BigDecimal valorVenda,
        BigDecimal larguraPeca,
        BigDecimal comprimentoPeca,
        UnidadeMedida unidadeMedida,
        BigDecimal areaPecaM2,
        Boolean ativo,
        LocalDateTime dataCadastro
) {
    public static MaterialResponse from(Material material, boolean incluirValorCusto) {
        return new MaterialResponse(
                material.getId(),
                material.getNome(),
                material.getDescricao(),
                material.getMarca(),
                material.getCor(),
                material.getQuantidade(),
                material.getEstoqueMinimo(),
                incluirValorCusto ? material.getValorCusto() : null,
                material.getValorVenda(),
                material.getLarguraPeca(),
                material.getComprimentoPeca(),
                material.getUnidadeMedida(),
                material.getAreaPecaM2(),
                material.getAtivo(),
                material.getDataCadastro()
        );
    }
}
