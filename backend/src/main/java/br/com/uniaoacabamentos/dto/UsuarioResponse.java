package br.com.uniaoacabamentos.dto;

import br.com.uniaoacabamentos.model.TipoUsuario;
import br.com.uniaoacabamentos.model.Usuario;

public record UsuarioResponse(Long id, String nome, String login, TipoUsuario tipoUsuario, Boolean ativo) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getLogin(), u.getTipoUsuario(), u.getAtivo());
    }
}
