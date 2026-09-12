package br.com.uniaoacabamentos.service;

import br.com.uniaoacabamentos.dto.MovimentacaoRequest;
import br.com.uniaoacabamentos.dto.MovimentacaoEdicaoRequest;
import br.com.uniaoacabamentos.dto.MovimentacaoResponse;
import br.com.uniaoacabamentos.model.*;
import br.com.uniaoacabamentos.repository.*;
import br.com.uniaoacabamentos.util.ValidacaoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final ObraRepository obraRepository;
    private final PermissaoService permissaoService;

    public MaterialService(MaterialRepository materialRepository,
                           MovimentacaoEstoqueRepository movimentacaoRepository,
                           ObraRepository obraRepository,
                           PermissaoService permissaoService) {
        this.materialRepository = materialRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.obraRepository = obraRepository;
        this.permissaoService = permissaoService;
    }

    public List<Material> listar() {
        return materialRepository.findAllAtivos();
    }

    public List<Material> listarExcluidos(Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        return materialRepository.findAllInativos();
    }

    public Material buscar(Long id) {
        return materialRepository.findAtivoById(id).orElseThrow(() -> new RuntimeException("Material não encontrado."));
    }

    public Material salvar(Material material, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        validar(material);
        validarECalcularDimensoes(material, true);
        preparar(material);

        if (material.getQuantidade() != 0) {
            throw new RuntimeException("A quantidade inicial deve ser zero. Registre entradas pela aba de entradas e retiradas.");
        }

        validarDuplicidade(material, null);

        material.setId(null);
        material.setAtivo(true);
        material.setDataCadastro(LocalDateTime.now());
        return materialRepository.save(material);
    }

    public Material atualizar(Long id, Material novo, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        validar(novo);
        Material atual = buscar(id);

        preservarDimensoesOmitidas(novo, atual);
        validarECalcularDimensoes(novo, false);
        preparar(novo);
        validarDuplicidade(novo, id);

        if (novo.getQuantidade() != null && !novo.getQuantidade().equals(atual.getQuantidade())) {
            throw new RuntimeException("Altere a quantidade de material através da aba de entradas e retiradas.");
        }

        atual.setNome(novo.getNome().trim());
        atual.setDescricao(limpar(novo.getDescricao()));
        atual.setMarca(limpar(novo.getMarca()));
        atual.setCor(limpar(novo.getCor()));
        atual.setEstoqueMinimo(novo.getEstoqueMinimo());
        atual.setValorCusto(novo.getValorCusto());
        atual.setValorVenda(novo.getValorVenda());
        atual.setLarguraPeca(novo.getLarguraPeca());
        atual.setComprimentoPeca(novo.getComprimentoPeca());
        atual.setUnidadeMedida(novo.getUnidadeMedida());
        atual.setAreaPecaM2(novo.getAreaPecaM2());
        atual.setLarguraPecaCmLegada(novo.getLarguraPecaCmLegada());
        atual.setComprimentoPecaCmLegada(novo.getComprimentoPecaCmLegada());
        atual.setAtivo(true);
        return materialRepository.save(atual);
    }

    public void excluir(Long id, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        Material material = buscar(id);
        material.setAtivo(false);
        materialRepository.save(material);
    }

    public Material restaurar(Long id, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        Material material = materialRepository.findInativoById(id)
                .orElseThrow(() -> new RuntimeException("Material excluído não encontrado."));
        validar(material);
        validarECalcularDimensoes(material, false);
        preparar(material);
        validarDuplicidade(material, id);
        material.setAtivo(true);
        return materialRepository.save(material);
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoResponse> historico() {
        return movimentacaoRepository.findAllByOrderByDataMovimentacaoDescIdDesc().stream()
                .map(MovimentacaoResponse::from)
                .toList();
    }

    @Transactional
    public MovimentacaoResponse movimentar(MovimentacaoRequest req, Usuario usuarioAutenticado) {
        if (req.materialId() == null) throw new RuntimeException("Material é obrigatório.");
        if (req.tipo() == null) throw new RuntimeException("Tipo de movimentação é obrigatório.");
        if (req.quantidade() == null || req.quantidade() <= 0)
            throw new RuntimeException("Quantidade deve ser maior que zero.");
        ValidacaoUtil.exigirTextoComLetra(req.observacao(), "Observação", true);

        Material material = materialRepository.findAtivoByIdForUpdate(req.materialId())
                .orElseThrow(() -> new RuntimeException("Material não encontrado."));
        permissaoService.validarMovimentacao(usuarioAutenticado, req.tipo());

        Obra obra = null;
        if (req.obraId() != null) {
            obra = obraRepository.findAtivaByIdForUpdate(req.obraId())
                    .orElseThrow(() -> new RuntimeException("Obra vinculada não encontrada."));
            if (obra.getStatus() == StatusObra.FINALIZADA || obra.getStatus() == StatusObra.CANCELADA) {
                throw new RuntimeException("Não é possível registrar uma movimentação para esta obra porque ela está com o status "
                        + obra.getStatus() + ".");
            }
        }

        if (req.tipo() == TipoMovimentacao.SAIDA) {
            atualizarSaldo(material, (long) material.getQuantidade() - req.quantidade());
        } else {
            atualizarSaldo(material, (long) material.getQuantidade() + req.quantidade());
        }

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setUsuario(usuarioAutenticado);
        movimentacao.setMaterial(material);
        movimentacao.setTipo(req.tipo());
        movimentacao.setQuantidade(req.quantidade());
        movimentacao.setObservacao(limpar(req.observacao()));
        movimentacao.setDataMovimentacao(LocalDateTime.now());

        if (obra != null) {
            movimentacao.setObra(obra);
        }

        materialRepository.save(material);
        return MovimentacaoResponse.from(movimentacaoRepository.save(movimentacao));
    }

    @Transactional
    public MovimentacaoResponse editarMovimentacao(Long id, MovimentacaoEdicaoRequest req,
                                                    Usuario usuarioAutenticado) {
        if (req.quantidade() == null || req.quantidade() <= 0) {
            throw new RuntimeException("Quantidade deve ser maior que zero.");
        }
        ValidacaoUtil.exigirTextoComLetra(req.observacao(), "Observação", true);

        MovimentacaoEstoque movimentacao = movimentacaoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Movimentação não encontrada."));
        validarPermissaoEdicao(movimentacao, usuarioAutenticado);

        Material material = materialRepository.findAtivoByIdForUpdate(movimentacao.getMaterial().getId())
                .orElseThrow(() -> new RuntimeException("Material da movimentação não está ativo."));

        long novoSaldo;
        if (movimentacao.getTipo() == TipoMovimentacao.ENTRADA) {
            novoSaldo = (long) material.getQuantidade() - movimentacao.getQuantidade() + req.quantidade();
        } else {
            novoSaldo = (long) material.getQuantidade() + movimentacao.getQuantidade() - req.quantidade();
        }
        atualizarSaldo(material, novoSaldo);

        movimentacao.setQuantidade(req.quantidade());
        movimentacao.setObservacao(limpar(req.observacao()));
        movimentacao.setDataUltimaAlteracao(LocalDateTime.now());
        materialRepository.save(material);
        return MovimentacaoResponse.from(movimentacaoRepository.save(movimentacao));
    }

    private void validarPermissaoEdicao(MovimentacaoEstoque movimentacao, Usuario usuarioAutenticado) {
        if (permissaoService.isAdministrador(usuarioAutenticado)) return;
        boolean retiradaPropria = movimentacao.getTipo() == TipoMovimentacao.SAIDA
                && movimentacao.getUsuario() != null
                && movimentacao.getUsuario().getId().equals(usuarioAutenticado.getId());
        if (!retiradaPropria) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário comum só pode editar as próprias retiradas.");
        }
    }

    private void atualizarSaldo(Material material, long saldoCalculado) {
        if (saldoCalculado < 0) {
            throw new RuntimeException("Estoque insuficiente para concluir a movimentação.");
        }
        if (saldoCalculado > Integer.MAX_VALUE) {
            throw new RuntimeException("A quantidade resultante ultrapassa o limite permitido.");
        }
        material.setQuantidade((int) saldoCalculado);
    }

    private void validarDuplicidade(Material material, Long idIgnorado) {
        boolean duplicado = materialRepository.findAllAtivos().stream()
                .filter(cadastrado -> idIgnorado == null || !cadastrado.getId().equals(idIgnorado))
                .anyMatch(cadastrado -> materialEhSemelhante(cadastrado, material));

        if (duplicado) {
            throw new RuntimeException("Já existe um material cadastrado com o mesmo nome, marca e cor. Para diferenciar o material, altere pelo menos um desses campos.");
        }
    }

    private boolean materialEhSemelhante(Material existente, Material novo) {
        String nomeExistente = normalizar(existente.getNome());
        String nomeNovo = normalizar(novo.getNome());
        String marcaExistente = normalizar(existente.getMarca());
        String marcaNovo = normalizar(novo.getMarca());
        String corExistente = normalizar(existente.getCor());
        String corNovo = normalizar(novo.getCor());

        return !nomeExistente.isBlank() && !nomeNovo.isBlank() && nomeExistente.equals(nomeNovo)
                && !marcaExistente.isBlank() && !marcaNovo.isBlank() && marcaExistente.equals(marcaNovo)
                && !corExistente.isBlank() && !corNovo.isBlank() && corExistente.equals(corNovo);
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

    private void preservarDimensoesOmitidas(Material novo, Material atual) {
        atual.migrarDimensoesLegadas();
        if (novo.getLarguraPeca() == null && novo.getComprimentoPeca() == null
                && novo.getUnidadeMedida() == null) {
            novo.setLarguraPeca(atual.getLarguraPeca());
            novo.setComprimentoPeca(atual.getComprimentoPeca());
            novo.setUnidadeMedida(atual.getUnidadeMedida());
        }
    }

    private void validarECalcularDimensoes(Material material, boolean obrigatorias) {
        material.migrarDimensoesLegadas();
        BigDecimal largura = material.getLarguraPeca();
        BigDecimal comprimento = material.getComprimentoPeca();

        if (largura == null && comprimento == null && material.getUnidadeMedida() == null && !obrigatorias) {
            material.setAreaPecaM2(null);
            return;
        }
        if (largura == null) throw new RuntimeException("Largura da peça é obrigatória.");
        if (comprimento == null) throw new RuntimeException("Comprimento da peça é obrigatório.");
        if (material.getUnidadeMedida() == null) throw new RuntimeException("Unidade de medida é obrigatória.");
        if (largura.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Largura da peça deve ser maior que zero.");
        }
        if (comprimento.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Comprimento da peça deve ser maior que zero.");
        }

        largura = largura.setScale(4, RoundingMode.HALF_UP);
        comprimento = comprimento.setScale(4, RoundingMode.HALF_UP);
        BigDecimal larguraMetros = material.getUnidadeMedida().paraMetros(largura);
        BigDecimal comprimentoMetros = material.getUnidadeMedida().paraMetros(comprimento);
        BigDecimal area = larguraMetros.multiply(comprimentoMetros)
                .setScale(6, RoundingMode.HALF_UP);
        if (area.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("As dimensões informadas resultam em uma área inválida para a peça.");
        }

        material.setLarguraPeca(largura);
        material.setComprimentoPeca(comprimento);
        material.setAreaPecaM2(area);
        material.sincronizarDimensoesLegadas(larguraMetros, comprimentoMetros);
    }

    private void validar(Material material) {
        ValidacaoUtil.exigirTextoComLetra(material.getNome(), "Nome do material", true);
        ValidacaoUtil.exigirTextoComLetra(material.getMarca(), "Marca do material", true);
        ValidacaoUtil.exigirTextoComLetra(material.getCor(), "Cor do material", true);
        ValidacaoUtil.exigirTextoComLetra(material.getDescricao(), "Descrição do material", true);

        if (material.getQuantidade() == null) throw new RuntimeException("Quantidade é obrigatória.");
        if (material.getEstoqueMinimo() == null) throw new RuntimeException("Estoque mínimo é obrigatório.");
        if (material.getValorCusto() == null) throw new RuntimeException("Valor de custo é obrigatório.");
        if (material.getValorVenda() == null) throw new RuntimeException("Valor de venda é obrigatório.");

        ValidacaoUtil.exigirInteiroNaoNegativo(material.getQuantidade(), "Quantidade");
        ValidacaoUtil.exigirInteiroNaoNegativo(material.getEstoqueMinimo(), "Estoque mínimo");
        ValidacaoUtil.exigirDecimalNaoNegativo(material.getValorCusto(), "Valor de custo");
        ValidacaoUtil.exigirDecimalNaoNegativo(material.getValorVenda(), "Valor de venda");
    }

    private String limpar(String texto) {
        return texto == null ? null : texto.trim();
    }
}
