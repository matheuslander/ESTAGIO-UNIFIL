package br.com.uniaoacabamentos.controller;

import br.com.uniaoacabamentos.dto.UsuarioResponse;
import br.com.uniaoacabamentos.model.Usuario;
import br.com.uniaoacabamentos.service.UsuarioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> listar(@RequestParam Long usuarioLogadoId) {
        return service.listar(usuarioLogadoId).stream().map(UsuarioResponse::from).toList();
    }

    @PostMapping
    public UsuarioResponse salvar(@RequestBody Usuario u, @RequestParam Long usuarioLogadoId) {
        return UsuarioResponse.from(service.salvar(u, usuarioLogadoId));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @RequestBody Usuario u, @RequestParam Long usuarioLogadoId) {
        return UsuarioResponse.from(service.atualizar(id, u, usuarioLogadoId));
    }

    @PatchMapping("/{id}/status")
    public UsuarioResponse status(@PathVariable Long id, @RequestParam Boolean ativo, @RequestParam Long usuarioLogadoId) {
        return UsuarioResponse.from(service.status(id, ativo, usuarioLogadoId));
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id, @RequestParam Long usuarioLogadoId) {
        service.excluir(id, usuarioLogadoId);
    }
}
