package br.com.uniaoacabamentos.service;

import br.com.uniaoacabamentos.dto.OrcamentoMontadorRequest;
import br.com.uniaoacabamentos.dto.OrcamentoRequest;
import br.com.uniaoacabamentos.dto.OrcamentoResponse;
import br.com.uniaoacabamentos.model.*;
import br.com.uniaoacabamentos.repository.MaterialRepository;
import br.com.uniaoacabamentos.repository.ObraRepository;
import br.com.uniaoacabamentos.repository.OrcamentoRepository;
import br.com.uniaoacabamentos.util.ValidacaoUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
public class OrcamentoService {
    private static final int ESCALA_DIMENSAO = 4;
    private static final int ESCALA_AREA = 6;
    private static final int ESCALA_MONETARIA = 2;

    private final OrcamentoRepository orcamentoRepository;
    private final MaterialRepository materialRepository;
    private final ObraRepository obraRepository;
    private final PermissaoService permissaoService;

    public OrcamentoService(OrcamentoRepository orcamentoRepository,
                            MaterialRepository materialRepository,
                            ObraRepository obraRepository,
                            PermissaoService permissaoService) {
        this.orcamentoRepository = orcamentoRepository;
        this.materialRepository = materialRepository;
        this.obraRepository = obraRepository;
        this.permissaoService = permissaoService;
    }

    @Transactional(readOnly = true)
    public List<OrcamentoResponse> listar(Usuario usuarioAutenticado) {
        List<Orcamento> orcamentos = permissaoService.isAdministrador(usuarioAutenticado)
                ? orcamentoRepository.findAllByOrderByDataCriacaoDesc()
                : orcamentoRepository.findByUsuarioCriador_IdOrderByDataCriacaoDesc(usuarioAutenticado.getId());
        return orcamentos.stream().map(OrcamentoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrcamentoResponse> listarDisponiveisParaObra(Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        return orcamentoRepository
                .findByTipoOrcamentoAndStatusAndObraIsNullOrderByDataCriacaoDesc(
                        TipoOrcamento.OBRA, StatusOrcamento.CALCULADO)
                .stream().map(OrcamentoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrcamentoResponse calcular(OrcamentoRequest request, Usuario usuarioAutenticado) {
        Orcamento calculado = novoOrcamento(usuarioAutenticado, TipoOrcamento.OBRA);
        preencherCalculoObra(calculado, request);
        return OrcamentoResponse.from(calculado);
    }

    @Transactional
    public OrcamentoResponse criar(OrcamentoRequest request, Usuario usuarioAutenticado) {
        Orcamento orcamento = novoOrcamento(usuarioAutenticado, TipoOrcamento.OBRA);
        preencherCalculoObra(orcamento, request);
        return OrcamentoResponse.from(orcamentoRepository.save(orcamento));
    }

    @Transactional
    public OrcamentoResponse atualizar(Long id, OrcamentoRequest request, Usuario usuarioAutenticado) {
        Orcamento orcamento = buscarComBloqueio(id);
        exigirTipo(orcamento, TipoOrcamento.OBRA);
        exigirProprietarioOuAdministrador(orcamento, usuarioAutenticado);
        exigirCalculado(orcamento);
        if (orcamento.getObra() != null) {
            throw new RuntimeException("Orçamento já vinculado a uma obra não pode ser editado.");
        }
        preencherCalculoObra(orcamento, request);
        return OrcamentoResponse.from(orcamentoRepository.save(orcamento));
    }

    @Transactional(readOnly = true)
    public OrcamentoResponse calcularMontador(OrcamentoMontadorRequest request, Usuario usuarioAutenticado) {
        Orcamento calculado = novoOrcamento(usuarioAutenticado, TipoOrcamento.MONTADOR);
        preencherCalculoMontador(calculado, request, false);
        return OrcamentoResponse.from(calculado);
    }

    @Transactional
    public OrcamentoResponse criarMontador(OrcamentoMontadorRequest request, Usuario usuarioAutenticado) {
        Orcamento orcamento = novoOrcamento(usuarioAutenticado, TipoOrcamento.MONTADOR);
        preencherCalculoMontador(orcamento, request, true);
        return OrcamentoResponse.from(orcamentoRepository.save(orcamento));
    }

    @Transactional
    public OrcamentoResponse atualizarMontador(Long id, OrcamentoMontadorRequest request,
                                                Usuario usuarioAutenticado) {
        Orcamento orcamento = buscarComBloqueio(id);
        exigirTipo(orcamento, TipoOrcamento.MONTADOR);
        exigirProprietarioOuAdministrador(orcamento, usuarioAutenticado);
        exigirCalculado(orcamento);
        if (request == null || request.obraId() == null || orcamento.getObraMontador() == null
                || !Objects.equals(request.obraId(), orcamento.getObraMontador().getId())) {
            throw new RuntimeException("A obra do orçamento de montador não pode ser alterada.");
        }
        preencherCalculoMontador(orcamento, request, true);
        return OrcamentoResponse.from(orcamentoRepository.save(orcamento));
    }

    @Transactional
    public OrcamentoResponse alterarStatus(Long id, StatusOrcamento novoStatus,
                                            Usuario usuarioAutenticado) {
        Orcamento orcamento = buscarComBloqueio(id);
        if (novoStatus == null) throw new RuntimeException("O novo status do orçamento é obrigatório.");
        exigirCalculado(orcamento);

        if (novoStatus == StatusOrcamento.NAO_CONTRATADO) {
            exigirProprietarioOuAdministrador(orcamento, usuarioAutenticado);
            if (orcamento.getTipoOrcamento() == TipoOrcamento.OBRA && orcamento.getObra() != null) {
                throw new RuntimeException("Orçamento vinculado a uma obra não pode ser marcado como NÃO CONTRATADO.");
            }
            orcamento.setStatus(novoStatus);
            return OrcamentoResponse.from(orcamentoRepository.save(orcamento));
        }

        if (novoStatus == StatusOrcamento.CONTRATADO) {
            exigirProprietarioOuAdministrador(orcamento, usuarioAutenticado);
            exigirTipo(orcamento, TipoOrcamento.MONTADOR);
            Obra obra = obraRepository.findAtivaByIdForUpdate(orcamento.getObraMontador().getId())
                    .orElseThrow(() -> new RuntimeException("Obra vinculada não encontrada."));
            if (obra.getNomeMontador() != null || obra.getValorMontador() != null) {
                throw new RuntimeException("Esta obra já possui um montador contratado.");
            }
            obra.setNomeMontador(orcamento.getNomeMontador());
            obra.setValorMontador(orcamento.getValorTotal());
            orcamento.setStatus(StatusOrcamento.CONTRATADO);
            obraRepository.save(obra);
            return OrcamentoResponse.from(orcamentoRepository.save(orcamento));
        }

        throw new RuntimeException("Transição de status do orçamento não permitida.");
    }

    private Orcamento novoOrcamento(Usuario usuario, TipoOrcamento tipo) {
        Orcamento orcamento = new Orcamento();
        orcamento.setUsuarioCriador(usuario);
        orcamento.setTipoOrcamento(tipo);
        orcamento.setStatus(StatusOrcamento.CALCULADO);
        return orcamento;
    }

    private void preencherCalculoObra(Orcamento orcamento, OrcamentoRequest request) {
        if (request == null) throw new RuntimeException("Dados do orçamento são obrigatórios.");
        ValidacaoUtil.exigirTextoComLetra(request.nomeCliente(), "Nome do cliente", true);
        if (request.materialId() == null) throw new RuntimeException("Material é obrigatório.");

        BigDecimal larguraObra = dimensaoPositiva(request.larguraObraM(), "Largura da obra");
        BigDecimal comprimentoObra = dimensaoPositiva(request.comprimentoObraM(), "Comprimento da obra");
        Material material = materialRepository.findAtivoById(request.materialId())
                .orElseThrow(() -> new RuntimeException("Material ativo não encontrado."));
        material.migrarDimensoesLegadas();
        validarDimensoesMaterial(material);

        BigDecimal areaObra = larguraObra.multiply(comprimentoObra)
                .setScale(ESCALA_AREA, RoundingMode.HALF_UP);
        BigDecimal areaPeca = material.getAreaPecaM2().setScale(ESCALA_AREA, RoundingMode.HALF_UP);
        BigDecimal quantidadeCalculada = areaObra.divide(areaPeca, 0, RoundingMode.CEILING);
        if (quantidadeCalculada.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new RuntimeException("A quantidade calculada de peças ultrapassa o limite permitido.");
        }
        int quantidadePecas = quantidadeCalculada.intValueExact();
        BigDecimal valorUnitario = valorNaoNegativo(material.getValorVenda(), "valor de venda do material");
        BigDecimal valorTotal = valorUnitario.multiply(BigDecimal.valueOf(quantidadePecas))
                .setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);

        ItemOrcamento item = new ItemOrcamento();
        item.setMaterial(material);
        item.setNomeMaterial(material.getNome());
        item.setLarguraPeca(material.getLarguraPeca());
        item.setComprimentoPeca(material.getComprimentoPeca());
        item.setUnidadeMedida(material.getUnidadeMedida());
        item.setAreaPecaM2(areaPeca);
        item.setQuantidadePecas(quantidadePecas);
        item.setValorUnitario(valorUnitario);
        item.setSubtotal(valorTotal);
        orcamento.substituirItem(item);

        orcamento.setTipoOrcamento(TipoOrcamento.OBRA);
        orcamento.setNomeCliente(request.nomeCliente().trim());
        orcamento.setNomeMontador(null);
        orcamento.setMaterial(material);
        orcamento.setNomeMaterial(material.getNome());
        orcamento.setLarguraObraM(larguraObra);
        orcamento.setComprimentoObraM(comprimentoObra);
        orcamento.setAreaObraM2(areaObra);
        orcamento.setLarguraPecaCm(converterParaCm(material.getLarguraPeca(), material.getUnidadeMedida()));
        orcamento.setComprimentoPecaCm(converterParaCm(material.getComprimentoPeca(), material.getUnidadeMedida()));
        orcamento.setAreaPecaM2(areaPeca);
        orcamento.setQuantidadePecas(quantidadePecas);
        orcamento.setValorUnitario(valorUnitario);
        orcamento.setMetragem(areaObra.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP));
        orcamento.setMetragemReferenciaM2(areaObra);
        orcamento.setValorPorMetroQuadrado(null);
        orcamento.setValorMateriais(valorTotal);
        orcamento.setValorMaoDeObra(BigDecimal.ZERO.setScale(ESCALA_MONETARIA));
        orcamento.setValorTotal(valorTotal);
        orcamento.setStatus(StatusOrcamento.CALCULADO);
    }

    private void preencherCalculoMontador(Orcamento orcamento, OrcamentoMontadorRequest request,
                                          boolean validarUnicidade) {
        if (request == null) throw new RuntimeException("Dados do orçamento de montador são obrigatórios.");
        if (request.obraId() == null) throw new RuntimeException("Obra é obrigatória.");
        ValidacaoUtil.exigirTextoComLetra(request.nomeMontador(), "Nome do montador", true);
        if (request.valorPorMetroQuadrado() == null || request.valorPorMetroQuadrado().signum() <= 0) {
            throw new RuntimeException("Valor por m² deve ser maior que zero.");
        }

        Obra obra = validarUnicidade
                ? obraRepository.findAtivaByIdForUpdate(request.obraId())
                    .orElseThrow(() -> new RuntimeException("Obra não encontrada."))
                : obraRepository.findAtivaById(request.obraId())
                    .orElseThrow(() -> new RuntimeException("Obra não encontrada."));
        Long idAtual = orcamento.getId();
        if (validarUnicidade && obra.getOrcamentoMontador() != null
                && !Objects.equals(obra.getOrcamentoMontador().getId(), idAtual)) {
            throw new RuntimeException("Esta obra já possui um orçamento de montador.");
        }
        if (obra.getMetragemM2() == null || obra.getMetragemM2().signum() <= 0) {
            throw new RuntimeException("A obra não possui metragem válida para calcular o montador.");
        }

        BigDecimal metragem = obra.getMetragemM2().setScale(ESCALA_AREA, RoundingMode.HALF_UP);
        BigDecimal valorM2 = request.valorPorMetroQuadrado().setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
        BigDecimal total = metragem.multiply(valorM2).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);

        orcamento.setTipoOrcamento(TipoOrcamento.MONTADOR);
        orcamento.setNomeCliente(obra.getNomeCliente());
        orcamento.setNomeMontador(request.nomeMontador().trim());
        orcamento.setObraMontador(obra);
        if (validarUnicidade) obra.setOrcamentoMontador(orcamento);
        orcamento.setMetragem(metragem.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP));
        orcamento.setMetragemReferenciaM2(metragem);
        orcamento.setValorPorMetroQuadrado(valorM2);
        orcamento.setValorMateriais(BigDecimal.ZERO.setScale(ESCALA_MONETARIA));
        orcamento.setValorMaoDeObra(total);
        orcamento.setValorTotal(total);
        orcamento.setStatus(StatusOrcamento.CALCULADO);
        orcamento.removerItensLegados();
    }

    private Orcamento buscarComBloqueio(Long id) {
        if (id == null) throw new RuntimeException("Orçamento é obrigatório.");
        return orcamentoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado."));
    }

    private void exigirCalculado(Orcamento orcamento) {
        if (orcamento.getStatus() != StatusOrcamento.CALCULADO) {
            throw new RuntimeException("Somente orçamento com status CALCULADO pode ser alterado.");
        }
    }

    private void exigirTipo(Orcamento orcamento, TipoOrcamento tipo) {
        if (orcamento.getTipoOrcamento() != tipo) {
            throw new RuntimeException("Operação incompatível com o tipo do orçamento.");
        }
    }

    private BigDecimal dimensaoPositiva(BigDecimal valor, String campo) {
        if (valor == null) throw new RuntimeException(campo + " é obrigatória.");
        if (valor.signum() <= 0) throw new RuntimeException(campo + " deve ser maior que zero.");
        return valor.setScale(ESCALA_DIMENSAO, RoundingMode.HALF_UP);
    }

    private BigDecimal valorNaoNegativo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() < 0) {
            throw new RuntimeException("O " + campo + " não é válido.");
        }
        return valor.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    private void validarDimensoesMaterial(Material material) {
        if (material.getLarguraPeca() == null || material.getComprimentoPeca() == null
                || material.getUnidadeMedida() == null || material.getAreaPecaM2() == null
                || material.getLarguraPeca().signum() <= 0 || material.getComprimentoPeca().signum() <= 0
                || material.getAreaPecaM2().signum() <= 0) {
            throw new RuntimeException("Este material não possui dimensões cadastradas. Atualize o cadastro do material antes de utilizá-lo em um orçamento.");
        }
    }

    private BigDecimal converterParaCm(BigDecimal valor, UnidadeMedida unidade) {
        return switch (unidade) {
            case MM -> valor.divide(BigDecimal.TEN, ESCALA_DIMENSAO, RoundingMode.HALF_UP);
            case CM -> valor.setScale(ESCALA_DIMENSAO, RoundingMode.HALF_UP);
            case M -> valor.multiply(BigDecimal.valueOf(100)).setScale(ESCALA_DIMENSAO, RoundingMode.HALF_UP);
        };
    }

    private void exigirProprietarioOuAdministrador(Orcamento orcamento, Usuario usuario) {
        boolean proprietario = orcamento.getUsuarioCriador() != null
                && Objects.equals(orcamento.getUsuarioCriador().getId(), usuario.getId());
        if (!permissaoService.isAdministrador(usuario) && !proprietario) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário só pode alterar os próprios orçamentos.");
        }
    }
}
