package br.com.uniaoacabamentos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String nome;
    @Column(nullable = false, unique = true)
    private String login;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String senha;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoUsuario tipoUsuario;
    @Enumerated(EnumType.STRING)
    private StatusUsuario status;
    private String caminhoFoto;
    private LocalDateTime ultimoAcesso;
    /** Coluna legada sincronizada com status para preservar dados existentes. */
    private Boolean ativo = true;
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    public void sincronizarCompatibilidade() {
        if (status == null) status = Boolean.FALSE.equals(ativo) ? StatusUsuario.INATIVO : StatusUsuario.ATIVO;
        ativo = status == StatusUsuario.ATIVO;
        if (tipoUsuario != null) tipoUsuario = tipoUsuario.normalizado();
        if (dataCriacao == null) dataCriacao = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public TipoUsuario getTipoUsuario() {
        return tipoUsuario;
    }

    public void setTipoUsuario(TipoUsuario tipoUsuario) {
        this.tipoUsuario = tipoUsuario;
    }

    public StatusUsuario getStatus() {
        return status == null
                ? (Boolean.FALSE.equals(ativo) ? StatusUsuario.INATIVO : StatusUsuario.ATIVO)
                : status;
    }

    public boolean temStatusPersistido() {
        return status != null;
    }

    public void setStatus(StatusUsuario status) {
        this.status = status;
        if (status != null) this.ativo = status == StatusUsuario.ATIVO;
    }

    public String getCaminhoFoto() {
        return caminhoFoto;
    }

    public void setCaminhoFoto(String caminhoFoto) {
        this.caminhoFoto = caminhoFoto;
    }

    public LocalDateTime getUltimoAcesso() {
        return ultimoAcesso;
    }

    public void setUltimoAcesso(LocalDateTime ultimoAcesso) {
        this.ultimoAcesso = ultimoAcesso;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
        if (ativo != null) this.status = ativo ? StatusUsuario.ATIVO : StatusUsuario.INATIVO;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
