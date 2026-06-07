package br.com.uniaoacabamentos.service;

import br.com.uniaoacabamentos.dto.MovimentacaoRequest;
import br.com.uniaoacabamentos.model.*;
import br.com.uniaoacabamentos.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObraRepository obraRepository;
    private final ItemOrcamentoRepository itemOrcamentoRepository;
    private final PermissaoService permissaoService;

    public MaterialService(MaterialRepository materialRepository,
                           MovimentacaoEstoqueRepository movimentacaoRepository,
                           UsuarioRepository usuarioRepository,
                           ObraRepository obraRepository,
                           ItemOrcamentoRepository itemOrcamentoRepository,
                           PermissaoService permissaoService) {
        this.materialRepository = materialRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.obraRepository = obraRepository;
        this.itemOrcamentoRepository = itemOrcamentoRepository;
        this.permissaoService = permissaoService;
    }

    private Usuario usuario(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
    }

    public List<Material> listar() {
        return materialRepository.findAll();
    }

    public Material buscar(Long id) {
        return materialRepository.findById(id).orElseThrow(() -> new RuntimeException("Material não encontrado."));
    }

    public Material salvar(Material material, Long usuarioId) {
        permissaoService.exigirAdministrador(usuario(usuarioId));
        preparar(material);
        validar(material);

        validarDuplicidade(material, null);

        material.setId(null);
        material.setDataCadastro(LocalDateTime.now());
        return materialRepository.save(material);
    }

    public Material atualizar(Long id, Material novo, Long usuarioId) {
        permissaoService.exigirAdministrador(usuario(usuarioId));
        preparar(novo);
        validar(novo);

        validarDuplicidade(novo, id);

        Material atual = buscar(id);
        atual.setNome(novo.getNome().trim());
        atual.setDescricao(limpar(novo.getDescricao()));
        atual.setMarca(limpar(novo.getMarca()));
        atual.setCor(limpar(novo.getCor()));
        atual.setQuantidade(novo.getQuantidade());
        atual.setEstoqueMinimo(novo.getEstoqueMinimo());
        atual.setValorCusto(novo.getValorCusto());
        atual.setValorVenda(novo.getValorVenda());
        return materialRepository.save(atual);
    }

    public void excluir(Long id, Long usuarioId) {
        permissaoService.exigirAdministrador(usuario(usuarioId));
        Material material = buscar(id);

        if (movimentacaoRepository.countByMaterialId(id) > 0) {
            throw new RuntimeException("Este material possui movimentações de estoque e não pode ser excluído.");
        }

        if (itemOrcamentoRepository.countByMaterialId(id) > 0) {
            throw new RuntimeException("Este material está vinculado a orçamentos e não pode ser excluído.");
        }

        materialRepository.delete(material);
    }

    public List<MovimentacaoEstoque> historico() {
        return movimentacaoRepository.findAll();
    }

    @Transactional
    public MovimentacaoEstoque movimentar(MovimentacaoRequest req) {
        Usuario usuario = usuario(req.usuarioId());
        Material material = buscar(req.materialId());
        permissaoService.validarMovimentacao(usuario, req.tipo());

        if (req.tipo() == null) throw new RuntimeException("Tipo de movimentação é obrigatório.");
        if (req.quantidade() == null || req.quantidade() <= 0)
            throw new RuntimeException("Quantidade deve ser maior que zero.");

        if (req.tipo() == TipoMovimentacao.SAIDA) {
            if (material.getQuantidade() < req.quantidade()) {
                throw new RuntimeException("Estoque insuficiente para registrar a retirada.");
            }
            material.setQuantidade(material.getQuantidade() - req.quantidade());
        } else {
            material.setQuantidade(material.getQuantidade() + req.quantidade());
        }

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setUsuario(usuario);
        movimentacao.setMaterial(material);
        movimentacao.setTipo(req.tipo());
        movimentacao.setQuantidade(req.quantidade());
        movimentacao.setObservacao(limpar(req.observacao()));

        if (req.obraId() != null) {
            Obra obra = obraRepository.findById(req.obraId()).orElseThrow(() -> new RuntimeException("Obra vinculada não encontrada."));
            movimentacao.setObra(obra);
        }

        materialRepository.save(material);
        return movimentacaoRepository.save(movimentacao);
    }

    private void validarDuplicidade(Material material, Long idIgnorado) {
        boolean duplicado = materialRepository.findAll().stream()
                .filter(cadastrado -> idIgnorado == null || !cadastrado.getId().equals(idIgnorado))
                .anyMatch(cadastrado -> materialEhSemelhante(cadastrado, material));

        if (duplicado) {
            throw new RuntimeException("Já existe um material cadastrado com o mesmo nome e a mesma cor ou descrição. Materiais iguais só podem ser cadastrados quando a cor e a descrição forem diferentes.");
        }
    }

    private boolean materialEhSemelhante(Material existente, Material novo) {
        String nomeExistente = normalizar(existente.getNome());
        String nomeNovo = normalizar(novo.getNome());
        String marcaExistente = normalizar(existente.getMarca());
        String marcaNovo = normalizar(novo.getMarca());
        String corExistente = normalizar(existente.getCor());
        String corNovo = normalizar(novo.getCor());
        String descricaoExistente = normalizar(existente.getDescricao());
        String descricaoNovo = normalizar(novo.getDescricao());

        if (nomeExistente.isBlank() || nomeNovo.isBlank() || !nomeExistente.equals(nomeNovo)) return false;
        if (!marcaExistente.isBlank() && !marcaNovo.isBlank() && !marcaExistente.equals(marcaNovo)) return false;

        boolean mesmaCorPreenchida = !corExistente.isBlank() && !corNovo.isBlank() && corExistente.equals(corNovo);
        boolean mesmaDescricaoPreenchida = !descricaoExistente.isBlank() && !descricaoNovo.isBlank() && descricaoExistente.equals(descricaoNovo);

        return mesmaCorPreenchida || mesmaDescricaoPreenchida;
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    private void preparar(Material material) {
        if (material.getQuantidade() == null) material.setQuantidade(0);
        if (material.getEstoqueMinimo() == null) material.setEstoqueMinimo(0);
        if (material.getValorCusto() == null) material.setValorCusto(BigDecimal.ZERO);
        if (material.getValorVenda() == null) material.setValorVenda(BigDecimal.ZERO);
    }

    private void validar(Material material) {
        if (material.getNome() == null || material.getNome().isBlank())
            throw new RuntimeException("Nome do material é obrigatório.");
        if (material.getQuantidade() < 0) throw new RuntimeException("Quantidade não pode ser negativa.");
        if (material.getEstoqueMinimo() < 0) throw new RuntimeException("Estoque mínimo não pode ser negativo.");
        if (material.getValorCusto().compareTo(BigDecimal.ZERO) < 0)
            throw new RuntimeException("Valor de custo não pode ser negativo.");
        if (material.getValorVenda().compareTo(BigDecimal.ZERO) < 0)
            throw new RuntimeException("Valor de venda não pode ser negativo.");
    }

    private String limpar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
