package br.com.uniaoacabamentos.controller;

import br.com.uniaoacabamentos.dto.ObraRequest;
import br.com.uniaoacabamentos.dto.ObraResponse;
import br.com.uniaoacabamentos.dto.StatusRequest;
import br.com.uniaoacabamentos.service.AutenticacaoService;
import br.com.uniaoacabamentos.service.ObraService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/obras")
public class ObraController {
    private final ObraService service;
    private final AutenticacaoService autenticacaoService;

    public ObraController(ObraService service, AutenticacaoService autenticacaoService) {
        this.service = service;
        this.autenticacaoService = autenticacaoService;
    }

    @GetMapping
    public List<ObraResponse> listar(HttpServletRequest request) {
        autenticacaoService.exigirUsuarioAutenticado(request);
        return service.listar();
    }

    @GetMapping("/{id}")
    public ObraResponse buscar(@PathVariable Long id, HttpServletRequest request) {
        autenticacaoService.exigirUsuarioAutenticado(request);
        return service.buscar(id);
    }

    @PostMapping
    public ObraResponse salvar(@RequestBody ObraRequest obra, HttpServletRequest request) {
        return service.salvar(obra, autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PutMapping("/{id}")
    public ObraResponse atualizar(@PathVariable Long id, @RequestBody ObraRequest obra,
                                  HttpServletRequest request) {
        return service.atualizar(id, obra, autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PatchMapping("/{id}/status")
    public ObraResponse status(@PathVariable Long id, @RequestBody StatusRequest status,
                               HttpServletRequest request) {
        return service.alterarStatus(id, status.status(),
                autenticacaoService.exigirUsuarioAutenticado(request));
    }
}
