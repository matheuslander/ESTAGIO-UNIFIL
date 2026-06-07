package br.com.uniaoacabamentos.controller;

import br.com.uniaoacabamentos.dto.OrcamentoRequest;
import br.com.uniaoacabamentos.model.Orcamento;
import br.com.uniaoacabamentos.service.OrcamentoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orcamentos")
public class OrcamentoController {
    private final OrcamentoService service;

    public OrcamentoController(OrcamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Orcamento> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Orcamento buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public Orcamento criar(@RequestBody OrcamentoRequest req) {
        return service.criar(req);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id, @RequestParam Long usuarioLogadoId) {
        service.excluir(id, usuarioLogadoId);
    }
}
