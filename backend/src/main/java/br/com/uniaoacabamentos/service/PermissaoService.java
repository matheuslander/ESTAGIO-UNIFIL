package br.com.uniaoacabamentos.service;

import br.com.uniaoacabamentos.model.TipoMovimentacao;
import br.com.uniaoacabamentos.model.TipoUsuario;
import br.com.uniaoacabamentos.model.Usuario;
import org.springframework.stereotype.Service;

@Service
public class PermissaoService {
    public boolean isAdministrador(Usuario usuario) {
        return usuario != null && usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR;
    }

    public void exigirAdministrador(Usuario usuario) {
        if (!isAdministrador(usuario)) throw new RuntimeException("Ação permitida apenas para administrador.");
    }

    public void validarMovimentacao(Usuario usuario, TipoMovimentacao tipo) {
        if (tipo == TipoMovimentacao.ENTRADA && !isAdministrador(usuario)) {
            throw new RuntimeException("Usuário comum não pode registrar entrada de materiais.");
        }
    }
}
