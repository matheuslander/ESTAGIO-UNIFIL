package br.com.uniaoacabamentos.controller;

import br.com.uniaoacabamentos.dto.LoginRequest;
import br.com.uniaoacabamentos.dto.UsuarioResponse;
import br.com.uniaoacabamentos.service.AutenticacaoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AutenticacaoService autenticacaoService;

    public AuthController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @PostMapping("/login")
    public UsuarioResponse login(@RequestBody LoginRequest req, HttpServletRequest request) {
        var usuario = autenticacaoService.autenticar(req);
        autenticacaoService.iniciarSessao(request, usuario);
        return UsuarioResponse.from(usuario);
    }

    @GetMapping("/me")
    public UsuarioResponse me(HttpServletRequest request) {
        return UsuarioResponse.from(autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        autenticacaoService.encerrarSessao(request);
    }
}
