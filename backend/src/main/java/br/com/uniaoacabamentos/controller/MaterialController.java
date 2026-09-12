package br.com.uniaoacabamentos.controller;

import br.com.uniaoacabamentos.dto.MovimentacaoEdicaoRequest;
import br.com.uniaoacabamentos.dto.MaterialResponse;
import br.com.uniaoacabamentos.dto.MovimentacaoRequest;
import br.com.uniaoacabamentos.dto.MovimentacaoResponse;
import br.com.uniaoacabamentos.model.*;
import br.com.uniaoacabamentos.service.AutenticacaoService;
import br.com.uniaoacabamentos.service.MaterialService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materiais")
public class MaterialController {
    private final MaterialService service;
    private final AutenticacaoService autenticacaoService;

    public MaterialController(MaterialService service, AutenticacaoService autenticacaoService) {
        this.service = service;
        this.autenticacaoService = autenticacaoService;
    }

    @GetMapping
    public List<MaterialResponse> listar(HttpServletRequest request) {
        Usuario usuario = autenticacaoService.exigirUsuarioAutenticado(request);
        boolean administrador = isAdministrador(usuario);
        return service.listar().stream()
                .map(material -> MaterialResponse.from(material, administrador))
                .toList();
    }

    @GetMapping("/excluidos")
    public List<MaterialResponse> listarExcluidos(HttpServletRequest request) {
        Usuario usuario = autenticacaoService.exigirUsuarioAutenticado(request);
        return service.listarExcluidos(usuario).stream()
                .map(material -> MaterialResponse.from(material, true))
                .toList();
    }

    @GetMapping("/{id}")
    public MaterialResponse buscar(@PathVariable Long id, HttpServletRequest request) {
        Usuario usuario = autenticacaoService.exigirUsuarioAutenticado(request);
        return MaterialResponse.from(service.buscar(id), isAdministrador(usuario));
    }

    @PostMapping
    public MaterialResponse salvar(@RequestBody Material m, HttpServletRequest request) {
        Usuario usuario = autenticacaoService.exigirUsuarioAutenticado(request);
        return MaterialResponse.from(service.salvar(m, usuario), true);
    }

    @PutMapping("/{id}")
    public MaterialResponse atualizar(@PathVariable Long id, @RequestBody Material m, HttpServletRequest request) {
        Usuario usuario = autenticacaoService.exigirUsuarioAutenticado(request);
        return MaterialResponse.from(service.atualizar(id, m, usuario), true);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id, HttpServletRequest request) {
        service.excluir(id, autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PatchMapping("/{id}/restaurar")
    public MaterialResponse restaurar(@PathVariable Long id, HttpServletRequest request) {
        Usuario usuario = autenticacaoService.exigirUsuarioAutenticado(request);
        return MaterialResponse.from(service.restaurar(id, usuario), true);
    }

    @GetMapping("/movimentacoes")
    public List<MovimentacaoResponse> historico(HttpServletRequest request) {
        autenticacaoService.exigirUsuarioAutenticado(request);
        return service.historico();
    }

    @PostMapping("/movimentacoes")
    public MovimentacaoResponse movimentar(@RequestBody MovimentacaoRequest req, HttpServletRequest request) {
        return service.movimentar(req, autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PutMapping("/movimentacoes/{id}")
    public MovimentacaoResponse editarMovimentacao(@PathVariable Long id,
                                                    @RequestBody MovimentacaoEdicaoRequest req,
                                                    HttpServletRequest request) {
        return service.editarMovimentacao(id, req, autenticacaoService.exigirUsuarioAutenticado(request));
    }

    private boolean isAdministrador(Usuario usuario) {
        return usuario != null && usuario.getTipoUsuario() != null
                && usuario.getTipoUsuario().normalizado() == TipoUsuario.ADMINISTRADOR;
    }
}
