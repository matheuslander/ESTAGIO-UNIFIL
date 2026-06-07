package br.com.uniaoacabamentos.controller;

import br.com.uniaoacabamentos.dto.LoginRequest;
import br.com.uniaoacabamentos.dto.UsuarioResponse;
import br.com.uniaoacabamentos.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public UsuarioResponse login(@RequestBody LoginRequest req) {
        return repository.findByLogin(req.login())
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .filter(u -> passwordEncoder.matches(req.senha(), u.getSenha()))
                .map(UsuarioResponse::from)
                .orElseThrow(() -> new RuntimeException("Login ou senha inválidos."));
    }
}
