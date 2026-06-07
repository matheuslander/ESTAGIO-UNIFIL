package br.com.uniaoacabamentos.model;

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
    private BigDecimal valorOrcamento = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusObra status = StatusObra.CADASTRADA;
    private LocalDateTime dataInicio;
    private LocalDateTime dataCriacao = LocalDateTime.now();

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

    public BigDecimal getValorOrcamento() {
        return valorOrcamento;
    }

    public void setValorOrcamento(BigDecimal valorOrcamento) {
        this.valorOrcamento = valorOrcamento;
    }

    public StatusObra getStatus() {
        return status;
    }

    public void setStatus(StatusObra status) {
        this.status = status;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
