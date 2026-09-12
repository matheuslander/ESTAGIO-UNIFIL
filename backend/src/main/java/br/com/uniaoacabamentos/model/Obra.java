package br.com.uniaoacabamentos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "obra")
public class Obra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nomeObra;
    @Column(nullable = false)
    private String nomeCliente;
    private String endereco;
    private String descricao;
    private Double metragem;
    @Column(precision = 19, scale = 2)
    private BigDecimal valorContratado;
    private String nomeMontador;
    @Column(precision = 19, scale = 2)
    private BigDecimal valorMontador;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusObra status = StatusObra.CADASTRADA;
    private Boolean ativo = true;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFinalizacao;
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @JsonIgnore
    @OneToOne(mappedBy = "obra", fetch = FetchType.LAZY)
    private Orcamento orcamento;

    @JsonIgnore
    @OneToOne(mappedBy = "obraMontador", fetch = FetchType.LAZY)
    private Orcamento orcamentoMontador;

    @PrePersist
    public void prePersist() {
        if (ativo == null) ativo = true;
        if (dataCriacao == null) dataCriacao = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeObra() {
        return nomeObra;
    }

    public void setNomeObra(String nomeObra) {
        this.nomeObra = nomeObra;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Double getMetragem() {
        return metragem;
    }

    public void setMetragem(Double metragem) {
        this.metragem = metragem;
    }

    public BigDecimal getMetragemM2() {
        return metragem == null ? null : BigDecimal.valueOf(metragem);
    }

    public BigDecimal getValorContratado() { return valorContratado; }
    public void setValorContratado(BigDecimal valorContratado) { this.valorContratado = valorContratado; }
    public String getNomeMontador() { return nomeMontador; }
    public void setNomeMontador(String nomeMontador) { this.nomeMontador = nomeMontador; }
    public BigDecimal getValorMontador() { return valorMontador; }
    public void setValorMontador(BigDecimal valorMontador) { this.valorMontador = valorMontador; }


    public StatusObra getStatus() {
        return status;
    }

    public void setStatus(StatusObra status) {
        this.status = status;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDateTime getDataFinalizacao() { return dataFinalizacao; }
    public void setDataFinalizacao(LocalDateTime dataFinalizacao) { this.dataFinalizacao = dataFinalizacao; }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public Orcamento getOrcamento() {
        return orcamento;
    }

    public void setOrcamento(Orcamento orcamento) {
        this.orcamento = orcamento;
    }

    public Orcamento getOrcamentoMontador() { return orcamentoMontador; }
    public void setOrcamentoMontador(Orcamento orcamentoMontador) { this.orcamentoMontador = orcamentoMontador; }
}
