package br.com.uniaoacabamentos.service;

import br.com.uniaoacabamentos.dto.LoginRequest;
import br.com.uniaoacabamentos.model.StatusUsuario;
import br.com.uniaoacabamentos.model.Usuario;
import br.com.uniaoacabamentos.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AutenticacaoService {
    private static final String USUARIO_ID_SESSAO = "UNICONTROL_USUARIO_ID";
    private static final String USUARIO_REQUISICAO = "UNICONTROL_USUARIO_AUTENTICADO";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AutenticacaoService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario autenticar(LoginRequest requisicao) {
        if (requisicao == null || requisicao.login() == null || requisicao.login().isBlank()
                || requisicao.senha() == null || requisicao.senha().isBlank()) {
            throw new RuntimeException("Login e senha são obrigatórios.");
        }

        Usuario usuario = usuarioRepository.findByLoginIgnoreCase(requisicao.login().trim())
                .orElseThrow(() -> new RuntimeException("Login ou senha inválidos."));
        if (usuario.getStatus() == StatusUsuario.INATIVO) {
            throw new RuntimeException("Usuário inativo. Entre em contato com o administrador.");
        }
        if (!passwordEncoder.matches(requisicao.senha(), usuario.getSenha())) {
            throw new RuntimeException("Login ou senha inválidos.");
        }

        usuario.setUltimoAcesso(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    public void iniciarSessao(HttpServletRequest request, Usuario usuario) {
        HttpSession sessaoAtual = request.getSession(false);
        if (sessaoAtual != null) sessaoAtual.invalidate();
        request.getSession(true).setAttribute(USUARIO_ID_SESSAO, usuario.getId());
    }

    public Usuario exigirUsuarioAutenticado(HttpServletRequest request) {
        Object usuarioEmCache = request.getAttribute(USUARIO_REQUISICAO);
        if (usuarioEmCache instanceof Usuario usuario) return usuario;

        HttpSession sessao = request.getSession(false);
        if (sessao == null || !(sessao.getAttribute(USUARIO_ID_SESSAO) instanceof Long usuarioId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Faça login para continuar.");
        }

        Usuario usuario = usuarioRepository.findAtivoById(usuarioId, StatusUsuario.ATIVO).orElseThrow(() -> {
            sessao.invalidate();
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida ou usuário inativo.");
        });
        request.setAttribute(USUARIO_REQUISICAO, usuario);
        return usuario;
    }

    public void encerrarSessao(HttpServletRequest request) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) sessao.invalidate();
    }
}
