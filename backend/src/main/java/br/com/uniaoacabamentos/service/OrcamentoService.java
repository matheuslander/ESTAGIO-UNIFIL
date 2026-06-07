package br.com.uniaoacabamentos.service;

import br.com.uniaoacabamentos.dto.OrcamentoItemRequest;
import br.com.uniaoacabamentos.dto.OrcamentoRequest;
import br.com.uniaoacabamentos.model.*;
import br.com.uniaoacabamentos.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrcamentoService {
    private final OrcamentoRepository orcamentoRepository;
    private final MaterialRepository materialRepository;
    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private final PermissaoService permissaoService;

    public OrcamentoService(OrcamentoRepository orcamentoRepository,
                            MaterialRepository materialRepository,
                            ObraRepository obraRepository,
                            UsuarioRepository usuarioRepository,
                            PermissaoService permissaoService) {
        this.orcamentoRepository = orcamentoRepository;
        this.materialRepository = materialRepository;
        this.obraRepository = obraRepository;
        this.usuarioRepository = usuarioRepository;
        this.permissaoService = permissaoService;
    }

    public List<Orcamento> listar() {
        return orcamentoRepository.findAll();
    }

    public Orcamento buscar(Long id) {
        return orcamentoRepository.findById(id).orElseThrow(() -> new RuntimeException("Orçamento não encontrado."));
    }

    @Transactional
    public Orcamento criar(OrcamentoRequest req) {
        Usuario usuario = usuarioRepository.findById(req.usuarioId()).orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        Obra obra = obraRepository.findById(req.obraId()).orElseThrow(() -> new RuntimeException("Obra é obrigatória e deve existir."));

        if (req.tipoOrcamento() == null) throw new RuntimeException("Tipo de orçamento é obrigatório.");
        if (req.itens() == null || req.itens().isEmpty())
            throw new RuntimeException("O orçamento precisa ter pelo menos um item.");
        if (req.metragem() != null && req.metragem() < 0) throw new RuntimeException("Metragem não pode ser negativa.");

        Orcamento orcamento = new Orcamento();
        orcamento.setUsuario(usuario);
        orcamento.setObra(obra);
        orcamento.setTipoOrcamento(req.tipoOrcamento());
        orcamento.setMetragem(req.metragem() == null ? 0.0 : req.metragem());
        orcamento.setValorMaoObra(req.valorMaoObra() == null ? BigDecimal.ZERO : req.valorMaoObra());
        orcamento.setObservacao(req.observacao());

        BigDecimal valorMateriais = BigDecimal.ZERO;
        for (OrcamentoItemRequest itemReq : req.itens()) {
            if (itemReq.materialId() == null) throw new RuntimeException("Material do item é obrigatório.");
            if (itemReq.quantidade() == null || itemReq.quantidade() <= 0)
                throw new RuntimeException("Quantidade do item deve ser maior que zero.");

            Material material = materialRepository.findById(itemReq.materialId()).orElseThrow(() -> new RuntimeException("Material do orçamento não encontrado."));
            BigDecimal valorUnitario = material.getValorVenda() == null ? BigDecimal.ZERO : material.getValorVenda();
            BigDecimal subtotal = valorUnitario.multiply(BigDecimal.valueOf(itemReq.quantidade()));

            ItemOrcamento item = new ItemOrcamento();
            item.setOrcamento(orcamento);
            item.setMaterial(material);
            item.setQuantidade(itemReq.quantidade());
            item.setValorUnitario(valorUnitario);
            item.setSubtotal(subtotal);

            orcamento.getItens().add(item);
            valorMateriais = valorMateriais.add(subtotal);
        }

        orcamento.setValorMateriais(valorMateriais);
        orcamento.setValorTotal(valorMateriais.add(orcamento.getValorMaoObra()));
        return orcamentoRepository.save(orcamento);
    }

    public void excluir(Long id, Long usuarioLogadoId) {
        Usuario usuario = usuarioRepository.findById(usuarioLogadoId).orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        permissaoService.exigirAdministrador(usuario);
        orcamentoRepository.delete(buscar(id));
    }
}
