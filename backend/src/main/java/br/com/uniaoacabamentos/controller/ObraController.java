package br.com.uniaoacabamentos.controller;

import br.com.uniaoacabamentos.dto.StatusRequest;
import br.com.uniaoacabamentos.model.*;
import br.com.uniaoacabamentos.service.ObraService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/obras")
public class ObraController {
    private final ObraService service;

    public ObraController(ObraService service) {
        this.service = service;
    }

    @GetMapping
    public List<Obra> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Obra buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public Obra salvar(@RequestBody Obra o, @RequestParam Long usuarioLogadoId) {
        return service.salvar(o, usuarioLogadoId);
    }

    @PutMapping("/{id}")
    public Obra atualizar(@PathVariable Long id, @RequestBody Obra o, @RequestParam Long usuarioLogadoId) {
        return service.atualizar(id, o, usuarioLogadoId);
    }

    @PatchMapping("/{id}/status")
    public Obra status(@PathVariable Long id, @RequestBody StatusRequest req, @RequestParam Long usuarioLogadoId) {
        return service.alterarStatus(id, req.status(), usuarioLogadoId);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id, @RequestParam Long usuarioLogadoId) {
        service.excluir(id, usuarioLogadoId);
    }
}
