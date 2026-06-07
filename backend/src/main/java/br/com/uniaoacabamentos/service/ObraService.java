package br.com.uniaoacabamentos.service;

import br.com.uniaoacabamentos.model.*;
import br.com.uniaoacabamentos.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Objects;

@Service
public class ObraService {
    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final PermissaoService permissaoService;

    public ObraService(ObraRepository obraRepository,
                       UsuarioRepository usuarioRepository,
                       MovimentacaoEstoqueRepository movimentacaoRepository,
                       OrcamentoRepository orcamentoRepository,
                       PermissaoService permissaoService) {
        this.obraRepository = obraRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.permissaoService = permissaoService;
    }

    private Usuario usuario(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
    }

    public List<Obra> listar() {
        return obraRepository.findAll();
    }

    public Obra buscar(Long id) {
        return obraRepository.findById(id).orElseThrow(() -> new RuntimeException("Obra não encontrada."));
    }

    public Obra salvar(Obra obra, Long usuarioId) {
        permissaoService.exigirAdministrador(usuario(usuarioId));
        preparar(obra);
        validar(obra);

        if (existeDuplicada(obra, null)) {
            throw new RuntimeException("Já existe uma obra semelhante cadastrada em aberto. Só é permitido repetir uma obra quando a anterior estiver finalizada.");
        }

        obra.setId(null);
        return obraRepository.save(obra);
    }

    public Obra atualizar(Long id, Obra novo, Long usuarioId) {
        permissaoService.exigirAdministrador(usuario(usuarioId));
        preparar(novo);
        validar(novo);

        if (existeDuplicada(novo, id)) {
            throw new RuntimeException("Já existe outra obra semelhante cadastrada em aberto. Só é permitido repetir uma obra quando a anterior estiver finalizada.");
        }

        Obra atual = buscar(id);
        atual.setNomeObra(novo.getNomeObra().trim());
        atual.setNomeCliente(novo.getNomeCliente().trim());
        atual.setEndereco(limpar(novo.getEndereco()));
        atual.setDescricao(limpar(novo.getDescricao()));
        atual.setMetragem(novo.getMetragem());
        atual.setValorOrcamento(novo.getValorOrcamento());
        atual.setStatus(novo.getStatus());
        atual.setDataInicio(novo.getDataInicio());
        return obraRepository.save(atual);
    }

    public Obra alterarStatus(Long id, StatusObra status, Long usuarioId) {
        permissaoService.exigirAdministrador(usuario(usuarioId));
        if (status == null) throw new RuntimeException("Status da obra é obrigatório.");
        Obra obra = buscar(id);
        obra.setStatus(status);
        return obraRepository.save(obra);
    }

    public void excluir(Long id, Long usuarioId) {
        permissaoService.exigirAdministrador(usuario(usuarioId));
        Obra obra = buscar(id);

        if (movimentacaoRepository.countByObraId(id) > 0) {
            throw new RuntimeException("Esta obra possui movimentações de estoque vinculadas e não pode ser excluída.");
        }

        if (orcamentoRepository.countByObraId(id) > 0) {
            throw new RuntimeException("Esta obra possui orçamentos vinculados e não pode ser excluída.");
        }

        obraRepository.delete(obra);
    }

    private boolean existeDuplicada(Obra obra, Long idAtual) {
        String nomeObra = normalizar(obra.getNomeObra());
        String nomeCliente = normalizar(obra.getNomeCliente());
        String endereco = normalizar(obra.getEndereco());
        String descricao = normalizar(obra.getDescricao());

        return obraRepository.findAll().stream().anyMatch(existente -> {
            if (idAtual != null && Objects.equals(existente.getId(), idAtual)) {
                return false;
            }

            if (existente.getStatus() == StatusObra.FINALIZADA) {
                return false;
            }

            return normalizar(existente.getNomeObra()).equals(nomeObra)
                    && normalizar(existente.getNomeCliente()).equals(nomeCliente)
                    && normalizar(existente.getEndereco()).equals(endereco)
                    && normalizar(existente.getDescricao()).equals(descricao);
        });
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    private void preparar(Obra obra) {
        if (obra.getStatus() == null) obra.setStatus(StatusObra.CADASTRADA);
        if (obra.getMetragem() == null) obra.setMetragem(0.0);
        if (obra.getValorOrcamento() == null) obra.setValorOrcamento(BigDecimal.ZERO);
        if (obra.getEndereco() == null) obra.setEndereco("");
    }

    private void validar(Obra obra) {
        if (obra.getNomeObra() == null || obra.getNomeObra().isBlank())
            throw new RuntimeException("Nome da obra é obrigatório.");
        if (obra.getNomeCliente() == null || obra.getNomeCliente().isBlank())
            throw new RuntimeException("Nome do cliente é obrigatório.");
        if (obra.getMetragem() < 0) throw new RuntimeException("Metragem não pode ser negativa.");
        if (obra.getValorOrcamento().compareTo(BigDecimal.ZERO) < 0)
            throw new RuntimeException("Valor do orçamento não pode ser negativo.");
    }

    private String limpar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
