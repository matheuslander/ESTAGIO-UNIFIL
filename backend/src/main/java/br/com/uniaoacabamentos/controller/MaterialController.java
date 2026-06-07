package br.com.uniaoacabamentos.controller;

import br.com.uniaoacabamentos.dto.MovimentacaoRequest;
import br.com.uniaoacabamentos.model.*;
import br.com.uniaoacabamentos.service.MaterialService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materiais")
public class MaterialController {
    private final MaterialService service;

    public MaterialController(MaterialService service) {
        this.service = service;
    }

    @GetMapping
    public List<Material> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Material buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public Material salvar(@RequestBody Material m, @RequestParam Long usuarioLogadoId) {
        return service.salvar(m, usuarioLogadoId);
    }

    @PutMapping("/{id}")
    public Material atualizar(@PathVariable Long id, @RequestBody Material m, @RequestParam Long usuarioLogadoId) {
        return service.atualizar(id, m, usuarioLogadoId);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id, @RequestParam Long usuarioLogadoId) {
        service.excluir(id, usuarioLogadoId);
    }

    @GetMapping("/movimentacoes")
    public List<MovimentacaoEstoque> historico() {
        return service.historico();
    }

    @PostMapping("/movimentacoes")
    public MovimentacaoEstoque movimentar(@RequestBody MovimentacaoRequest req) {
        return service.movimentar(req);
    }
}
