package br.com.uniaoacabamentos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "material")
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome;
    private String descricao;
    private String marca;
    private String cor;
    private Integer quantidade = 0;
    private Integer estoqueMinimo = 0;
    private BigDecimal valorCusto = BigDecimal.ZERO;
    private BigDecimal valorVenda = BigDecimal.ZERO;

    @Column(precision = 15, scale = 4)
    private BigDecimal larguraPeca;
    @Column(precision = 15, scale = 4)
    private BigDecimal comprimentoPeca;
    @Enumerated(EnumType.STRING)
    private UnidadeMedida unidadeMedida;
    @Column(precision = 15, scale = 6)
    private BigDecimal areaPecaM2;

    /** Colunas da versão que armazenava todas as medidas em centímetros. */
    @JsonIgnore
    @Column(name = "largura_peca_cm", precision = 15, scale = 4)
    private BigDecimal larguraPecaCmLegada;
    @JsonIgnore
    @Column(name = "comprimento_peca_cm", precision = 15, scale = 4)
    private BigDecimal comprimentoPecaCmLegada;

    private Boolean ativo = true;
    private LocalDateTime dataCadastro = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (ativo == null) ativo = true;
        if (dataCadastro == null) dataCadastro = LocalDateTime.now();
    }

    public boolean migrarDimensoesLegadas() {
        if (larguraPeca == null && comprimentoPeca == null && unidadeMedida == null
                && larguraPecaCmLegada != null && comprimentoPecaCmLegada != null) {
            larguraPeca = larguraPecaCmLegada;
            comprimentoPeca = comprimentoPecaCmLegada;
            unidadeMedida = UnidadeMedida.CM;
            return true;
        }
        return false;
    }

    public void sincronizarDimensoesLegadas(BigDecimal larguraMetros, BigDecimal comprimentoMetros) {
        larguraPecaCmLegada = larguraMetros.multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
        comprimentoPecaCmLegada = comprimentoMetros.multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public Integer getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(Integer estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }
    public BigDecimal getValorCusto() { return valorCusto; }
    public void setValorCusto(BigDecimal valorCusto) { this.valorCusto = valorCusto; }
    public BigDecimal getValorVenda() { return valorVenda; }
    public void setValorVenda(BigDecimal valorVenda) { this.valorVenda = valorVenda; }
    public BigDecimal getLarguraPeca() { return larguraPeca; }
    public void setLarguraPeca(BigDecimal larguraPeca) { this.larguraPeca = larguraPeca; }
    public BigDecimal getComprimentoPeca() { return comprimentoPeca; }
    public void setComprimentoPeca(BigDecimal comprimentoPeca) { this.comprimentoPeca = comprimentoPeca; }
    public UnidadeMedida getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(UnidadeMedida unidadeMedida) { this.unidadeMedida = unidadeMedida; }
    public BigDecimal getAreaPecaM2() { return areaPecaM2; }
    public void setAreaPecaM2(BigDecimal areaPecaM2) { this.areaPecaM2 = areaPecaM2; }
    public BigDecimal getLarguraPecaCmLegada() { return larguraPecaCmLegada; }
    public void setLarguraPecaCmLegada(BigDecimal valor) { this.larguraPecaCmLegada = valor; }
    public BigDecimal getComprimentoPecaCmLegada() { return comprimentoPecaCmLegada; }
    public void setComprimentoPecaCmLegada(BigDecimal valor) { this.comprimentoPecaCmLegada = valor; }
    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
}
