package br.com.uniaoacabamentos.controller;

import br.com.uniaoacabamentos.dto.OrcamentoRequest;
import br.com.uniaoacabamentos.dto.OrcamentoMontadorRequest;
import br.com.uniaoacabamentos.dto.OrcamentoResponse;
import br.com.uniaoacabamentos.dto.StatusOrcamentoRequest;
import br.com.uniaoacabamentos.model.Usuario;
import br.com.uniaoacabamentos.service.AutenticacaoService;
import br.com.uniaoacabamentos.service.OrcamentoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orcamentos")
public class OrcamentoController {
    private final OrcamentoService orcamentoService;
    private final AutenticacaoService autenticacaoService;

    public OrcamentoController(OrcamentoService orcamentoService,
                               AutenticacaoService autenticacaoService) {
        this.orcamentoService = orcamentoService;
        this.autenticacaoService = autenticacaoService;
    }

    @GetMapping
    public List<OrcamentoResponse> listar(HttpServletRequest request) {
        return orcamentoService.listar(autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @GetMapping("/disponiveis-para-obra")
    public List<OrcamentoResponse> listarDisponiveisParaObra(HttpServletRequest request) {
        return orcamentoService.listarDisponiveisParaObra(
                autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PostMapping("/calcular")
    public OrcamentoResponse calcular(@RequestBody OrcamentoRequest orcamento,
                                       HttpServletRequest request) {
        return orcamentoService.calcular(orcamento,
                autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponse criar(@RequestBody OrcamentoRequest orcamento,
                                    HttpServletRequest request) {
        return orcamentoService.criar(orcamento,
                autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PutMapping("/{id}")
    public OrcamentoResponse atualizar(@PathVariable Long id,
                                       @RequestBody OrcamentoRequest orcamento,
                                       HttpServletRequest request) {
        return orcamentoService.atualizar(id, orcamento,
                autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PostMapping("/montador/calcular")
    public OrcamentoResponse calcularMontador(@RequestBody OrcamentoMontadorRequest orcamento,
                                               HttpServletRequest request) {
        return orcamentoService.calcularMontador(orcamento,
                autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PostMapping("/montador")
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponse criarMontador(@RequestBody OrcamentoMontadorRequest orcamento,
                                            HttpServletRequest request) {
        return orcamentoService.criarMontador(orcamento,
                autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PutMapping("/montador/{id}")
    public OrcamentoResponse atualizarMontador(@PathVariable Long id,
                                                @RequestBody OrcamentoMontadorRequest orcamento,
                                                HttpServletRequest request) {
        return orcamentoService.atualizarMontador(id, orcamento,
                autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PatchMapping("/{id}/status")
    public OrcamentoResponse alterarStatus(@PathVariable Long id,
                                            @RequestBody StatusOrcamentoRequest status,
                                            HttpServletRequest request) {
        return orcamentoService.alterarStatus(id, status.status(),
                autenticacaoService.exigirUsuarioAutenticado(request));
    }
}
