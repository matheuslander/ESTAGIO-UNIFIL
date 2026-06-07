package br.com.uniaoacabamentos.service;

import br.com.uniaoacabamentos.model.Usuario;
import br.com.uniaoacabamentos.repository.OrcamentoRepository;
import br.com.uniaoacabamentos.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final OrcamentoRepository orcamentoRepository;
    private final PermissaoService permissaoService;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository,
                          OrcamentoRepository orcamentoRepository,
                          PermissaoService permissaoService,
                          PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.orcamentoRepository = orcamentoRepository;
        this.permissaoService = permissaoService;
        this.passwordEncoder = passwordEncoder;
    }

    private Usuario logado(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Usuário logado não encontrado."));
    }

    public List<Usuario> listar(Long logadoId) {
        permissaoService.exigirAdministrador(logado(logadoId));
        return repository.findAll();
    }

    public Usuario salvar(Usuario usuario, Long logadoId) {
        permissaoService.exigirAdministrador(logado(logadoId));
        validar(usuario, null, true);

        if (repository.existsByLogin(usuario.getLogin().trim())) {
            throw new RuntimeException("Já existe um usuário cadastrado com esse login.");
        }

        usuario.setId(null);
        usuario.setNome(usuario.getNome().trim());
        usuario.setLogin(usuario.getLogin().trim());
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        if (usuario.getAtivo() == null) usuario.setAtivo(true);
        return repository.save(usuario);
    }

    public Usuario atualizar(Long id, Usuario novo, Long logadoId) {
        permissaoService.exigirAdministrador(logado(logadoId));
        validar(novo, id, false);

        if (repository.existsByLoginAndIdNot(novo.getLogin().trim(), id)) {
            throw new RuntimeException("Já existe outro usuário cadastrado com esse login.");
        }

        Usuario atual = repository.findById(id).orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        atual.setNome(novo.getNome().trim());
        atual.setLogin(novo.getLogin().trim());

        if (novo.getSenha() != null && !novo.getSenha().isBlank()) {
            atual.setSenha(passwordEncoder.encode(novo.getSenha()));
        }

        atual.setTipoUsuario(novo.getTipoUsuario());
        atual.setAtivo(novo.getAtivo() == null ? true : novo.getAtivo());
        return repository.save(atual);
    }

    public Usuario status(Long id, Boolean ativo, Long logadoId) {
        permissaoService.exigirAdministrador(logado(logadoId));

        if (id.equals(logadoId) && Boolean.FALSE.equals(ativo)) {
            throw new RuntimeException("O usuário logado não pode inativar a própria conta.");
        }

        Usuario usuario = repository.findById(id).orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        usuario.setAtivo(ativo);
        return repository.save(usuario);
    }

    public void excluir(Long id, Long logadoId) {
        permissaoService.exigirAdministrador(logado(logadoId));

        if (id.equals(logadoId)) {
            throw new RuntimeException("O usuário logado não pode excluir a própria conta.");
        }

        if (orcamentoRepository.countByUsuarioId(id) > 0) {
            throw new RuntimeException("Este usuário possui orçamentos vinculados e não pode ser excluído.");
        }

        repository.deleteById(id);
    }

    private void validar(Usuario usuario, Long idAtual, boolean exigirSenha) {
        if (usuario.getNome() == null || usuario.getNome().isBlank()) throw new RuntimeException("Nome obrigatório.");
        if (usuario.getLogin() == null || usuario.getLogin().isBlank())
            throw new RuntimeException("Login obrigatório.");
        if (exigirSenha && (usuario.getSenha() == null || usuario.getSenha().isBlank()))
            throw new RuntimeException("Senha obrigatória.");
        if (usuario.getTipoUsuario() == null) throw new RuntimeException("Tipo de usuário obrigatório.");
    }
}
