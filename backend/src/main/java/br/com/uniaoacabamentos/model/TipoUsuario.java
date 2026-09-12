package br.com.uniaoacabamentos.model;

public enum TipoUsuario {
    ADMINISTRADOR,
    USUARIO,
    /** Valor legado mantido apenas para leitura/migração de registros antigos. */
    USUARIO_COMUM;

    public TipoUsuario normalizado() {
        return this == USUARIO_COMUM ? USUARIO : this;
    }
}
