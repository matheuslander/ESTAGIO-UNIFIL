package br.com.uniaoacabamentos.service;

import br.com.uniaoacabamentos.model.StatusUsuario;
import br.com.uniaoacabamentos.model.TipoUsuario;
import br.com.uniaoacabamentos.model.Usuario;
import br.com.uniaoacabamentos.repository.UsuarioRepository;
import br.com.uniaoacabamentos.util.ValidacaoUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PermissaoService permissaoService;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PermissaoService permissaoService,
                          PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.permissaoService = permissaoService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listar(Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        return repository.findAtivos(StatusUsuario.ATIVO);
    }

    public List<Usuario> listarExcluidos(Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        return repository.findInativos(StatusUsuario.INATIVO);
    }

    @Transactional
    public Usuario salvar(Usuario usuario, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        validar(usuario, true);
        String loginNormalizado = normalizarLogin(usuario.getLogin());
        if (repository.existsByLoginIgnoreCase(loginNormalizado)) {
            throw new RuntimeException("Já existe um usuário cadastrado com este login.");
        }

        usuario.setId(null);
        usuario.setNome(usuario.getNome().trim());
        usuario.setLogin(loginNormalizado);
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setTipoUsuario(usuario.getTipoUsuario().normalizado());
        usuario.setStatus(StatusUsuario.ATIVO);
        usuario.setCaminhoFoto(null);
        usuario.setUltimoAcesso(null);
        return repository.save(usuario);
    }

    @Transactional
    public Usuario atualizar(Long id, Usuario novo, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        validar(novo, false);
        String loginNormalizado = normalizarLogin(novo.getLogin());
        if (repository.existsByLoginIgnoreCaseAndIdNot(loginNormalizado, id)) {
            throw new RuntimeException("Já existe um usuário cadastrado com este login.");
        }

        Usuario atual = repository.findAtivoById(id, StatusUsuario.ATIVO)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        TipoUsuario novoTipo = novo.getTipoUsuario().normalizado();
        if (atual.getTipoUsuario() == TipoUsuario.ADMINISTRADOR && novoTipo != TipoUsuario.ADMINISTRADOR) {
            garantirOutroAdministradorAtivo(atual);
        }

        atual.setNome(novo.getNome().trim());
        atual.setLogin(loginNormalizado);
        if (novo.getSenha() != null && !novo.getSenha().isBlank()) {
            atual.setSenha(passwordEncoder.encode(novo.getSenha()));
        }
        atual.setTipoUsuario(novoTipo);
        return repository.save(atual);
    }

    @Transactional
    public Usuario status(Long id, StatusUsuario status, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        if (status == null) throw new RuntimeException("Status do usuário é obrigatório.");
        if (id.equals(usuarioAutenticado.getId()) && status == StatusUsuario.INATIVO) {
            throw new RuntimeException("O usuário logado não pode inativar a própria conta.");
        }

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        if (status == StatusUsuario.INATIVO && usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            garantirOutroAdministradorAtivo(usuario);
        }
        usuario.setStatus(status);
        return repository.save(usuario);
    }

    public void inativar(Long id, Usuario usuarioAutenticado) {
        status(id, StatusUsuario.INATIVO, usuarioAutenticado);
    }

    @Transactional
    public Usuario restaurar(Long id, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        Usuario usuario = repository.findInativoById(id, StatusUsuario.INATIVO)
                .orElseThrow(() -> new RuntimeException("Usuário inativo não encontrado."));
        usuario.setStatus(StatusUsuario.ATIVO);
        return repository.save(usuario);
    }

    private void garantirOutroAdministradorAtivo(Usuario usuarioAlterado) {
        if (usuarioAlterado.getTipoUsuario() == TipoUsuario.ADMINISTRADOR
                && repository.bloquearAdministradoresAtivos(
                        TipoUsuario.ADMINISTRADOR, StatusUsuario.ATIVO).size() <= 1) {
            throw new RuntimeException("Não é possível realizar esta operação, pois o sistema deve possuir pelo menos um administrador ativo.");
        }
    }

    private String normalizarLogin(String login) {
        return login.trim().toLowerCase(Locale.ROOT);
    }

    private void validar(Usuario usuario, boolean exigirSenha) {
        if (usuario == null) throw new RuntimeException("Dados do usuário são obrigatórios.");
        if (usuario.getNome() == null || usuario.getNome().isBlank()) throw new RuntimeException("Nome obrigatório.");
        if (usuario.getLogin() == null || usuario.getLogin().isBlank()) throw new RuntimeException("Login obrigatório.");
        ValidacaoUtil.exigirTextoComLetra(usuario.getNome(), "Nome do usuário", true);
        ValidacaoUtil.exigirTextoComLetra(usuario.getLogin(), "Login do usuário", true);
        if (exigirSenha && (usuario.getSenha() == null || usuario.getSenha().isBlank())) {
            throw new RuntimeException("Senha obrigatória.");
        }
        if (usuario.getTipoUsuario() == null) throw new RuntimeException("Tipo de usuário obrigatório.");
    }
}
