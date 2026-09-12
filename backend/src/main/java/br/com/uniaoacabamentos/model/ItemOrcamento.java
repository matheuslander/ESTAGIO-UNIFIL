package br.com.uniaoacabamentos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "item_orcamento")
public class ItemOrcamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    private String nomeMaterial;

    @Column(precision = 15, scale = 4)
    private BigDecimal larguraPeca;

    @Column(precision = 15, scale = 4)
    private BigDecimal comprimentoPeca;

    @Enumerated(EnumType.STRING)
    private UnidadeMedida unidadeMedida;

    @Column(precision = 15, scale = 6)
    private BigDecimal areaPecaM2;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidadePecas;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorUnitario;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Orcamento getOrcamento() { return orcamento; }
    public void setOrcamento(Orcamento orcamento) { this.orcamento = orcamento; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public String getNomeMaterial() { return nomeMaterial; }
    public void setNomeMaterial(String nomeMaterial) { this.nomeMaterial = nomeMaterial; }
    public BigDecimal getLarguraPeca() { return larguraPeca; }
    public void setLarguraPeca(BigDecimal larguraPeca) { this.larguraPeca = larguraPeca; }
    public BigDecimal getComprimentoPeca() { return comprimentoPeca; }
    public void setComprimentoPeca(BigDecimal comprimentoPeca) { this.comprimentoPeca = comprimentoPeca; }
    public UnidadeMedida getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(UnidadeMedida unidadeMedida) { this.unidadeMedida = unidadeMedida; }
    public BigDecimal getAreaPecaM2() { return areaPecaM2; }
    public void setAreaPecaM2(BigDecimal areaPecaM2) { this.areaPecaM2 = areaPecaM2; }
    public Integer getQuantidadePecas() { return quantidadePecas; }
    public void setQuantidadePecas(Integer quantidadePecas) { this.quantidadePecas = quantidadePecas; }
    public Integer getQuantidade() { return quantidadePecas; }
    public void setQuantidade(Integer quantidade) { this.quantidadePecas = quantidade; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
