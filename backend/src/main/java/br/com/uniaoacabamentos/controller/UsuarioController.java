package br.com.uniaoacabamentos.controller;

import br.com.uniaoacabamentos.dto.UsuarioResponse;
import br.com.uniaoacabamentos.model.Usuario;
import br.com.uniaoacabamentos.model.StatusUsuario;
import br.com.uniaoacabamentos.service.FotoUsuarioService;
import br.com.uniaoacabamentos.service.UsuarioService;
import br.com.uniaoacabamentos.service.AutenticacaoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService service;
    private final AutenticacaoService autenticacaoService;
    private final FotoUsuarioService fotoUsuarioService;

    public UsuarioController(UsuarioService service, AutenticacaoService autenticacaoService,
                             FotoUsuarioService fotoUsuarioService) {
        this.service = service;
        this.autenticacaoService = autenticacaoService;
        this.fotoUsuarioService = fotoUsuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar(HttpServletRequest request) {
        return service.listar(autenticacaoService.exigirUsuarioAutenticado(request)).stream()
                .map(UsuarioResponse::from).toList();
    }

    @GetMapping("/excluidos")
    public List<UsuarioResponse> listarExcluidos(HttpServletRequest request) {
        return service.listarExcluidos(autenticacaoService.exigirUsuarioAutenticado(request)).stream()
                .map(UsuarioResponse::from).toList();
    }

    @PostMapping
    public UsuarioResponse salvar(@RequestBody Usuario u, HttpServletRequest request) {
        return UsuarioResponse.from(service.salvar(u, autenticacaoService.exigirUsuarioAutenticado(request)));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @RequestBody Usuario u, HttpServletRequest request) {
        return UsuarioResponse.from(service.atualizar(id, u, autenticacaoService.exigirUsuarioAutenticado(request)));
    }

    @PatchMapping("/{id}/status")
    public UsuarioResponse status(@PathVariable Long id,
                                  @RequestParam(required = false) StatusUsuario status,
                                  @RequestParam(required = false) Boolean ativo,
                                  HttpServletRequest request) {
        StatusUsuario statusResolvido = status != null ? status
                : ativo == null ? null : ativo ? StatusUsuario.ATIVO : StatusUsuario.INATIVO;
        return UsuarioResponse.from(service.status(id, statusResolvido,
                autenticacaoService.exigirUsuarioAutenticado(request)));
    }

    @DeleteMapping("/{id}")
    public void inativar(@PathVariable Long id, HttpServletRequest request) {
        service.inativar(id, autenticacaoService.exigirUsuarioAutenticado(request));
    }

    @PatchMapping("/{id}/restaurar")
    public UsuarioResponse restaurar(@PathVariable Long id, HttpServletRequest request) {
        return UsuarioResponse.from(service.restaurar(id, autenticacaoService.exigirUsuarioAutenticado(request)));
    }

    @PutMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UsuarioResponse atualizarFoto(@PathVariable Long id,
                                         @RequestPart("foto") MultipartFile foto,
                                         HttpServletRequest request) {
        return UsuarioResponse.from(fotoUsuarioService.salvar(id, foto,
                autenticacaoService.exigirUsuarioAutenticado(request)));
    }

    @DeleteMapping("/{id}/foto")
    public UsuarioResponse removerFoto(@PathVariable Long id, HttpServletRequest request) {
        return UsuarioResponse.from(fotoUsuarioService.remover(id,
                autenticacaoService.exigirUsuarioAutenticado(request)));
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> foto(@PathVariable Long id, HttpServletRequest request) {
        FotoUsuarioService.FotoArmazenada foto = fotoUsuarioService.carregar(id,
                autenticacaoService.exigirUsuarioAutenticado(request));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(foto.contentType()))
                .body(foto.recurso());
    }
}
