package br.com.uniaoacabamentos.model;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOrcamento tipoOrcamento = TipoOrcamento.CLIENTE;

    private Double metragem = 0.0;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMateriais = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMaoObra = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    private String observacao;
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @ManyToOne(optional = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    private Obra obra;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemOrcamento> itens = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoOrcamento getTipoOrcamento() {
        return tipoOrcamento;
    }

    public void setTipoOrcamento(TipoOrcamento tipoOrcamento) {
        this.tipoOrcamento = tipoOrcamento;
    }

    public Double getMetragem() {
        return metragem;
    }

    public void setMetragem(Double metragem) {
        this.metragem = metragem;
    }

    public BigDecimal getValorMateriais() {
        return valorMateriais;
    }

    public void setValorMateriais(BigDecimal valorMateriais) {
        this.valorMateriais = valorMateriais;
    }

    public BigDecimal getValorMaoObra() {
        return valorMaoObra;
    }

    public void setValorMaoObra(BigDecimal valorMaoObra) {
        this.valorMaoObra = valorMaoObra;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Obra getObra() {
        return obra;
    }

    public void setObra(Obra obra) {
        this.obra = obra;
    }

    public List<ItemOrcamento> getItens() {
        return itens;
    }

    public void setItens(List<ItemOrcamento> itens) {
        this.itens = itens;
    }
}
