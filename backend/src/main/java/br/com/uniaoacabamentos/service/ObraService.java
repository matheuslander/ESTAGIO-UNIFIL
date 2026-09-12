package br.com.uniaoacabamentos.service;

import br.com.uniaoacabamentos.dto.ObraRequest;
import br.com.uniaoacabamentos.dto.ObraResponse;
import br.com.uniaoacabamentos.model.*;
import br.com.uniaoacabamentos.repository.ObraRepository;
import br.com.uniaoacabamentos.repository.OrcamentoRepository;
import br.com.uniaoacabamentos.util.ValidacaoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@Service
public class ObraService {
    private final ObraRepository obraRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final PermissaoService permissaoService;

    public ObraService(ObraRepository obraRepository,
                       OrcamentoRepository orcamentoRepository,
                       PermissaoService permissaoService) {
        this.obraRepository = obraRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.permissaoService = permissaoService;
    }

    @Transactional(readOnly = true)
    public List<ObraResponse> listar() {
        return obraRepository.findAllAtivas().stream().map(ObraResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ObraResponse buscar(Long id) {
        return ObraResponse.from(buscarEntidade(id));
    }

    @Transactional
    public ObraResponse salvar(ObraRequest request, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        validarDadosProprios(request);
        if (request.orcamentoId() == null) {
            throw new RuntimeException("Orçamento de origem é obrigatório para cadastrar uma nova obra.");
        }

        Orcamento orcamento = orcamentoRepository.findByIdForUpdate(request.orcamentoId())
                .orElseThrow(() -> new RuntimeException("Orçamento de origem não encontrado."));
        validarOrcamentoDisponivel(orcamento);

        Obra obra = new Obra();
        obra.setNomeObra(request.nomeObra().trim());
        obra.setNomeCliente(orcamento.getNomeCliente().trim());
        obra.setEndereco(limpar(request.endereco()));
        obra.setDescricao(limpar(request.descricao()));
        obra.setMetragem(orcamento.getAreaObraM2().doubleValue());
        obra.setValorContratado(orcamento.getValorTotal());
        obra.setDataInicio(request.dataInicio());
        obra.setStatus(StatusObra.CADASTRADA);
        obra.setAtivo(true);

        validarDuplicidade(obra, null);
        Obra salva = obraRepository.save(obra);

        orcamento.setObra(salva);
        orcamento.setStatus(StatusOrcamento.CONTRATADO);
        salva.setOrcamento(orcamento);
        orcamentoRepository.save(orcamento);

        return ObraResponse.from(salva);
    }

    @Transactional
    public ObraResponse atualizar(Long id, ObraRequest request, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        validarDadosProprios(request);
        Obra atual = buscarEntidade(id);

        if (request.orcamentoId() != null) {
            Long orcamentoAtualId = atual.getOrcamento() == null ? null : atual.getOrcamento().getId();
            if (!Objects.equals(request.orcamentoId(), orcamentoAtualId)) {
                throw new RuntimeException("O orçamento de origem da obra não pode ser alterado.");
            }
        }

        Obra candidato = new Obra();
        candidato.setNomeObra(request.nomeObra().trim());
        candidato.setNomeCliente(atual.getNomeCliente());
        candidato.setEndereco(limpar(request.endereco()));
        validarDuplicidade(candidato, id);

        atual.setNomeObra(candidato.getNomeObra());
        atual.setEndereco(candidato.getEndereco());
        atual.setDescricao(limpar(request.descricao()));
        atual.setDataInicio(request.dataInicio());
        if (request.status() != null) aplicarTransicao(atual, request.status());
        atual.setAtivo(true);
        // Cliente, metragem e valores contratados permanecem os do orçamento.
        return ObraResponse.from(obraRepository.save(atual));
    }

    @Transactional
    public ObraResponse alterarStatus(Long id, StatusObra status, Usuario usuarioAutenticado) {
        permissaoService.exigirAdministrador(usuarioAutenticado);
        if (status == null) throw new RuntimeException("Status da obra é obrigatório.");
        Obra obra = buscarEntidade(id);
        aplicarTransicao(obra, status);
        return ObraResponse.from(obraRepository.save(obra));
    }

    private Obra buscarEntidade(Long id) {
        return obraRepository.findAtivaById(id)
                .orElseThrow(() -> new RuntimeException("Obra não encontrada."));
    }

    private void validarOrcamentoDisponivel(Orcamento orcamento) {
        if (orcamento.getTipoOrcamento() != TipoOrcamento.OBRA) {
            throw new RuntimeException("Somente orçamento de obra pode originar uma obra.");
        }
        if (orcamento.getStatus() != StatusOrcamento.CALCULADO) {
            throw new RuntimeException("Apenas orçamento com status CALCULADO pode originar uma obra.");
        }
        if (orcamento.getObra() != null) {
            throw new RuntimeException("Este orçamento já foi utilizado por outra obra.");
        }
        if (orcamento.getNomeCliente() == null || orcamento.getNomeCliente().isBlank()
                || orcamento.getAreaObraM2() == null || orcamento.getAreaObraM2().signum() <= 0
                || orcamento.getQuantidadePecas() == null || orcamento.getQuantidadePecas() <= 0
                || orcamento.getValorTotal() == null || orcamento.getValorTotal().signum() < 0) {
            throw new RuntimeException("O orçamento não possui todos os dados calculados necessários para cadastrar a obra.");
        }
    }

    private void validarDadosProprios(ObraRequest request) {
        if (request == null) throw new RuntimeException("Dados da obra são obrigatórios.");
        ValidacaoUtil.exigirTextoComLetra(request.nomeObra(), "Nome da obra", true);
        ValidacaoUtil.exigirTextoComLetra(request.endereco(), "Endereço", true);
        ValidacaoUtil.exigirTextoComLetra(request.descricao(), "Descrição da obra", true);
    }

    private void validarDuplicidade(Obra obra, Long idAtual) {
        if (obraRepository.contarDuplicadas(
                obra.getNomeObra().trim(), obra.getNomeCliente().trim(),
                obra.getEndereco() == null ? "" : obra.getEndereco().trim(), idAtual) > 0) {
            throw new RuntimeException("Já existe uma obra com o mesmo nome, cliente e endereço.");
        }
    }

    private String limpar(String texto) {
        return texto == null ? null : texto.trim();
    }

    private void aplicarTransicao(Obra obra, StatusObra novoStatus) {
        StatusObra atual = obra.getStatus();
        if (novoStatus == atual) return;

        EnumSet<StatusObra> permitidos = switch (atual) {
            case CADASTRADA -> EnumSet.of(StatusObra.EM_ANDAMENTO, StatusObra.CANCELADA);
            case EM_ANDAMENTO -> EnumSet.of(StatusObra.PAUSADA, StatusObra.FINALIZADA, StatusObra.CANCELADA);
            case PAUSADA -> EnumSet.of(StatusObra.EM_ANDAMENTO, StatusObra.CANCELADA);
            case FINALIZADA, CANCELADA -> EnumSet.noneOf(StatusObra.class);
        };
        if (!permitidos.contains(novoStatus)) {
            throw new RuntimeException("Transição de status não permitida: " + atual + " → " + novoStatus + ".");
        }
        obra.setStatus(novoStatus);
        if (novoStatus == StatusObra.FINALIZADA) {
            obra.setDataFinalizacao(LocalDateTime.now());
        }
    }
}
