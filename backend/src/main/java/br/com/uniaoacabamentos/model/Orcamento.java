package br.com.uniaoacabamentos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orcamento")
public class Orcamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeCliente;
    private String nomeMontador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    private String nomeMaterial;

    @Column(precision = 15, scale = 4)
    private BigDecimal larguraObraM;
    @Column(precision = 15, scale = 4)
    private BigDecimal comprimentoObraM;
    @Column(precision = 15, scale = 6)
    private BigDecimal areaObraM2;

    @Column(precision = 15, scale = 4)
    private BigDecimal larguraPecaCm;
    @Column(precision = 15, scale = 4)
    private BigDecimal comprimentoPecaCm;
    @Column(precision = 15, scale = 6)
    private BigDecimal areaPecaM2;

    private Integer quantidadePecas;

    @Column(precision = 19, scale = 2)
    private BigDecimal valorUnitario;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private StatusOrcamento status = StatusOrcamento.CALCULADO;

    @Column(nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_criador_id", nullable = false)
    private Usuario usuarioCriador;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "obra_id", unique = true, nullable = true)
    private Obra obra;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_montador_id", unique = true)
    private Obra obraMontador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOrcamento tipoOrcamento = TipoOrcamento.OBRA;

    @JsonIgnore
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal metragem = BigDecimal.ZERO;

    @Column(precision = 15, scale = 6)
    private BigDecimal metragemReferenciaM2;

    @Column(precision = 19, scale = 2)
    private BigDecimal valorPorMetroQuadrado;

    @JsonIgnore
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valorMateriais = BigDecimal.ZERO;

    @JsonIgnore
    @Column(name = "valor_mao_obra", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorMaoDeObra = BigDecimal.ZERO;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemOrcamento> itens = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (tipoOrcamento == null) tipoOrcamento = TipoOrcamento.OBRA;
        if (metragem == null) metragem = BigDecimal.ZERO;
        if (valorMateriais == null) valorMateriais = BigDecimal.ZERO;
        if (valorMaoDeObra == null) valorMaoDeObra = BigDecimal.ZERO;
        if (valorTotal == null) valorTotal = BigDecimal.ZERO;
        if (status == null) status = StatusOrcamento.CALCULADO;
        if (dataCriacao == null) dataCriacao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }
    public String getNomeMontador() { return nomeMontador; }
    public void setNomeMontador(String nomeMontador) { this.nomeMontador = nomeMontador; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public String getNomeMaterial() { return nomeMaterial; }
    public void setNomeMaterial(String nomeMaterial) { this.nomeMaterial = nomeMaterial; }
    public BigDecimal getLarguraObraM() { return larguraObraM; }
    public void setLarguraObraM(BigDecimal larguraObraM) { this.larguraObraM = larguraObraM; }
    public BigDecimal getComprimentoObraM() { return comprimentoObraM; }
    public void setComprimentoObraM(BigDecimal comprimentoObraM) { this.comprimentoObraM = comprimentoObraM; }
    public BigDecimal getAreaObraM2() { return areaObraM2; }
    public void setAreaObraM2(BigDecimal areaObraM2) { this.areaObraM2 = areaObraM2; }
    public BigDecimal getLarguraPecaCm() { return larguraPecaCm; }
    public void setLarguraPecaCm(BigDecimal larguraPecaCm) { this.larguraPecaCm = larguraPecaCm; }
    public BigDecimal getComprimentoPecaCm() { return comprimentoPecaCm; }
    public void setComprimentoPecaCm(BigDecimal comprimentoPecaCm) { this.comprimentoPecaCm = comprimentoPecaCm; }
    public BigDecimal getAreaPecaM2() { return areaPecaM2; }
    public void setAreaPecaM2(BigDecimal areaPecaM2) { this.areaPecaM2 = areaPecaM2; }
    public Integer getQuantidadePecas() { return quantidadePecas; }
    public void setQuantidadePecas(Integer quantidadePecas) { this.quantidadePecas = quantidadePecas; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public StatusOrcamento getStatus() { return status; }
    public void setStatus(StatusOrcamento status) { this.status = status; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    public Usuario getUsuarioCriador() { return usuarioCriador; }
    public void setUsuarioCriador(Usuario usuarioCriador) { this.usuarioCriador = usuarioCriador; }
    public Obra getObra() { return obra; }
    public void setObra(Obra obra) { this.obra = obra; }
    public Obra getObraMontador() { return obraMontador; }
    public void setObraMontador(Obra obraMontador) { this.obraMontador = obraMontador; }
    public TipoOrcamento getTipoOrcamento() { return tipoOrcamento; }
    public void setTipoOrcamento(TipoOrcamento tipoOrcamento) { this.tipoOrcamento = tipoOrcamento; }
    public BigDecimal getMetragem() { return metragem; }
    public void setMetragem(BigDecimal metragem) { this.metragem = metragem; }
    public BigDecimal getMetragemReferenciaM2() { return metragemReferenciaM2; }
    public void setMetragemReferenciaM2(BigDecimal metragemReferenciaM2) { this.metragemReferenciaM2 = metragemReferenciaM2; }
    public BigDecimal getValorPorMetroQuadrado() { return valorPorMetroQuadrado; }
    public void setValorPorMetroQuadrado(BigDecimal valorPorMetroQuadrado) { this.valorPorMetroQuadrado = valorPorMetroQuadrado; }
    public BigDecimal getValorMateriais() { return valorMateriais; }
    public void setValorMateriais(BigDecimal valorMateriais) { this.valorMateriais = valorMateriais; }
    public BigDecimal getValorMaoDeObra() { return valorMaoDeObra; }
    public void setValorMaoDeObra(BigDecimal valorMaoDeObra) { this.valorMaoDeObra = valorMaoDeObra; }
    public List<ItemOrcamento> getItens() { return itens; }
    public void substituirItem(ItemOrcamento item) {
        itens.clear();
        item.setOrcamento(this);
        itens.add(item);
    }
    public void removerItensLegados() { itens.clear(); }
}
