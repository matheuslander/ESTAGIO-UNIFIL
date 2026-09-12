package br.com.uniaoacabamentos.dto;

import br.com.uniaoacabamentos.model.TipoUsuario;
import br.com.uniaoacabamentos.model.StatusUsuario;
import br.com.uniaoacabamentos.model.Usuario;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nome,
        String login,
        TipoUsuario tipoUsuario,
        StatusUsuario status,
        Boolean ativo,
        String caminhoFoto,
        LocalDateTime dataCriacao,
        LocalDateTime ultimoAcesso
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(), u.getNome(), u.getLogin(), u.getTipoUsuario().normalizado(),
                u.getStatus(), u.getStatus() == StatusUsuario.ATIVO, u.getCaminhoFoto(),
                u.getDataCriacao(), u.getUltimoAcesso()
        );
    }
}
