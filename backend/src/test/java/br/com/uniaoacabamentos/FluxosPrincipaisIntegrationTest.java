package br.com.uniaoacabamentos;

import br.com.uniaoacabamentos.model.Material;
import br.com.uniaoacabamentos.repository.MaterialRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FluxosPrincipaisIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MaterialRepository materialRepository;

    @Test
    void deveCalcularDimensoesDoMaterialEManterQuantidadeProtegida() throws Exception {
        Login admin = login("admin", "admin123");

        JsonNode material = criarMaterial(admin.sessao(), "Piso Dimensão Teste", 0, "5.00", "40", "30");
        long materialId = material.get("id").asLong();
        assertThat(material.get("areaPecaM2").decimalValue()).isEqualByComparingTo("0.120000");

        mockMvc.perform(post("/api/materiais").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(materialJson("Largura Inválida", 0, "5.00", "0", "30")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Largura da peça deve ser maior que zero."));

        mockMvc.perform(post("/api/materiais").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(materialJson("Comprimento Inválido", 0, "5.00", "40", "-30")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Comprimento da peça deve ser maior que zero."));

        mockMvc.perform(put("/api/materiais/{id}", materialId).session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(materialJson("Piso Dimensão Teste", 1, "5.00", "40", "30")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("entradas e retiradas")));

        Material legado = new Material();
        legado.setNome("Material Legado Sem Dimensão");
        legado.setMarca("Marca Legado");
        legado.setCor("Cinza");
        legado.setDescricao("Registro anterior à nova regra");
        legado.setQuantidade(0);
        legado.setEstoqueMinimo(0);
        legado.setValorCusto(BigDecimal.ONE);
        legado.setValorVenda(BigDecimal.TEN);
        legado.setAtivo(true);
        legado = materialRepository.save(legado);

        mockMvc.perform(post("/api/orcamentos/calcular").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orcamentoJson("Cliente Legado", "2", "2", legado.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("não possui dimensões")));
    }

    @Test
    void deveCalcularOrcamentoContratarObraPreservarSnapshotESemMovimentarEstoque() throws Exception {
        Login admin = login("admin", "admin123");
        Login usuario = login("usuario", "usuario123");
        JsonNode material = criarMaterial(admin.sessao(), "Piso Vinílico Orçamento Teste", 0, "5.00", "40", "30");
        long materialId = material.get("id").asLong();

        mockMvc.perform(post("/api/materiais/movimentacoes").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimentoJson(materialId, "ENTRADA", 10, "Entrada para conferir estoque")))
                .andExpect(status().isOk());

        String joaoRequest = orcamentoJson("João da Silva", "10", "5", materialId);
        mockMvc.perform(post("/api/orcamentos/calcular").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON).content(joaoRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaObraM2").value(50.0))
                .andExpect(jsonPath("$.areaPecaM2").value(0.12))
                .andExpect(jsonPath("$.quantidadePecas").value(417))
                .andExpect(jsonPath("$.valorUnitario").value(5.0))
                .andExpect(jsonPath("$.valorTotal").value(2085.0));

        JsonNode joao = json(mockMvc.perform(post("/api/orcamentos").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON).content(joaoRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CALCULADO")).andReturn());
        long joaoId = joao.get("id").asLong();

        JsonNode maria = json(mockMvc.perform(post("/api/orcamentos").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orcamentoJson("Maria Oliveira", "12", "6", materialId)))
                .andExpect(status().isCreated()).andReturn());
        assertThat(maria.get("nomeCliente").asText()).isEqualTo("Maria Oliveira");

        mockMvc.perform(get("/api/orcamentos/disponiveis-para-obra").session(usuario.sessao()))
                .andExpect(status().isForbidden());

        JsonNode disponiveis = json(mockMvc.perform(get("/api/orcamentos/disponiveis-para-obra")
                        .session(admin.sessao()))
                .andExpect(status().isOk()).andReturn());
        assertThat(contemOrcamento(disponiveis, joaoId, "João da Silva")).isTrue();
        assertThat(contemOrcamento(disponiveis, maria.get("id").asLong(), "Maria Oliveira")).isTrue();

        mockMvc.perform(post("/api/obras").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeObra\":\"Obra sem orçamento\",\"endereco\":\"Rua A\",\"descricao\":\"Teste obrigatório\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("Orçamento de origem")));

        JsonNode obra = json(mockMvc.perform(post("/api/obras").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(obraJson(joaoId, "Obra João", "Rua Principal, 10", "Obra contratada", "FINALIZADA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeCliente").value("João da Silva"))
                .andExpect(jsonPath("$.metragem").value(50.0))
                .andExpect(jsonPath("$.status").value("CADASTRADA"))
                .andExpect(jsonPath("$.orcamentoOrigem.quantidadePecas").value(417))
                .andExpect(jsonPath("$.orcamentoOrigem.valorTotal").value(2085.0)).andReturn());
        assertThat(obra.get("orcamentoOrigem").get("id").asLong()).isEqualTo(joaoId);

        JsonNode aposContrato = json(mockMvc.perform(get("/api/orcamentos").session(admin.sessao()))
                .andExpect(status().isOk()).andReturn());
        JsonNode joaoContratado = localizarOrcamento(aposContrato, joaoId);
        assertThat(joaoContratado.get("status").asText()).isEqualTo("CONTRATADO");
        assertThat(joaoContratado.get("obraId").isNumber()).isTrue();

        mockMvc.perform(post("/api/obras").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(obraJson(joaoId, "Segunda Obra João", "Rua B", "Tentativa de reutilização", "CADASTRADA")))
                .andExpect(status().isBadRequest());

        String carlosRequest = orcamentoJson("Carlos Pereira", "4", "4", materialId);
        JsonNode carlos = json(mockMvc.perform(post("/api/orcamentos").session(usuario.sessao())
                        .contentType(MediaType.APPLICATION_JSON).content(carlosRequest))
                .andExpect(status().isCreated()).andReturn());
        long carlosId = carlos.get("id").asLong();

        mockMvc.perform(patch("/api/orcamentos/{id}/status", carlosId).session(usuario.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NAO_CONTRATADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NAO_CONTRATADO"));

        mockMvc.perform(post("/api/obras").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(obraJson(carlosId, "Obra Carlos", "Rua C", "Não deve criar", "CADASTRADA")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/materiais/{id}", materialId).session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(materialJson("Piso Vinílico Orçamento Teste", 10, "7.00", "40", "30")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorVenda").value(7.0));

        JsonNode historico = json(mockMvc.perform(get("/api/orcamentos").session(admin.sessao()))
                .andExpect(status().isOk()).andReturn());
        JsonNode snapshotJoao = localizarOrcamento(historico, joaoId);
        assertThat(snapshotJoao.get("valorUnitario").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(snapshotJoao.get("valorTotal").decimalValue()).isEqualByComparingTo("2085.00");

        mockMvc.perform(get("/api/materiais/{id}", materialId).session(admin.sessao()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(10));
    }

    @Test
    void deveExecutarFluxoCompletoDoOrcamentoDeObraSemVinculoInicial() throws Exception {
        Login admin = login("admin", "admin123");
        JsonNode material = criarMaterial(admin.sessao(), "Piso laminado cenário 20m²",
                0, "119.00", "19", "135");
        long materialId = material.get("id").asLong();
        String payload = orcamentoJson("Cliente cenário 20m²", "10", "2", materialId);

        mockMvc.perform(post("/api/orcamentos/calcular").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaObraM2").value(20.0))
                .andExpect(jsonPath("$.areaPecaM2").value(0.2565))
                .andExpect(jsonPath("$.quantidadePecas").value(78))
                .andExpect(jsonPath("$.valorUnitario").value(119.0))
                .andExpect(jsonPath("$.valorTotal").value(9282.0));

        JsonNode salvo = json(mockMvc.perform(post("/api/orcamentos").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoOrcamento").value("OBRA"))
                .andExpect(jsonPath("$.status").value("CALCULADO"))
                .andExpect(jsonPath("$.obraId").isEmpty())
                .andExpect(jsonPath("$.itens[0].larguraPeca").value(19.0))
                .andExpect(jsonPath("$.itens[0].comprimentoPeca").value(135.0))
                .andExpect(jsonPath("$.itens[0].unidadeMedida").value("CM"))
                .andExpect(jsonPath("$.itens[0].areaPecaM2").value(0.2565))
                .andExpect(jsonPath("$.itens[0].quantidadePecas").value(78))
                .andExpect(jsonPath("$.itens[0].valorUnitario").value(119.0))
                .andExpect(jsonPath("$.itens[0].subtotal").value(9282.0))
                .andReturn());
        long orcamentoId = salvo.get("id").asLong();

        JsonNode listado = localizarOrcamento(json(mockMvc.perform(get("/api/orcamentos")
                        .session(admin.sessao()))
                .andExpect(status().isOk()).andReturn()), orcamentoId);
        assertThat(listado.get("obraId").isNull()).isTrue();
        assertThat(listado.get("areaObraM2").decimalValue()).isEqualByComparingTo("20.000000");
        assertThat(listado.get("quantidadePecas").asInt()).isEqualTo(78);
        assertThat(listado.get("valorTotal").decimalValue()).isEqualByComparingTo("9282.00");

        JsonNode obra = json(mockMvc.perform(post("/api/obras").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(obraJson(orcamentoId, "Obra cenário 20m²", "Rua do Cenário, 20",
                                "Obra criada a partir do orçamento calculado", "CADASTRADA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CADASTRADA"))
                .andExpect(jsonPath("$.nomeCliente").value("Cliente cenário 20m²"))
                .andExpect(jsonPath("$.metragemM2").value(20.0))
                .andExpect(jsonPath("$.valorContratado").value(9282.0))
                .andExpect(jsonPath("$.orcamentoOrigem.id").value(orcamentoId))
                .andExpect(jsonPath("$.orcamentoOrigem.status").value("CONTRATADO"))
                .andReturn());
        long obraId = obra.get("id").asLong();

        JsonNode contratado = localizarOrcamento(json(mockMvc.perform(get("/api/orcamentos")
                        .session(admin.sessao()))
                .andExpect(status().isOk()).andReturn()), orcamentoId);
        assertThat(contratado.get("status").asText()).isEqualTo("CONTRATADO");
        assertThat(contratado.get("obraId").asLong()).isEqualTo(obraId);

        mockMvc.perform(post("/api/obras").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(obraJson(orcamentoId, "Segunda obra indevida", "Outra rua, 10",
                                "Tentativa de reutilizar orçamento", "CADASTRADA")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/orcamentos/montador").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeMontador\":\"Montador sem obra\",\"valorPorMetroQuadrado\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Obra é obrigatória."));

        mockMvc.perform(post("/api/orcamentos/montador").session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"obraId\":" + obraId
                                + ",\"nomeMontador\":\"Montador do cenário\",\"valorPorMetroQuadrado\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoOrcamento").value("MONTADOR"))
                .andExpect(jsonPath("$.status").value("CALCULADO"))
                .andExpect(jsonPath("$.obraId").value(obraId))
                .andExpect(jsonPath("$.metragemReferenciaM2").value(20.0))
                .andExpect(jsonPath("$.valorTotal").value(200.0));

        mockMvc.perform(get("/api/materiais/{id}", materialId).session(admin.sessao()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(0));
    }

    private Login login(String login, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Credenciais(login, senha))))
                .andExpect(status().isOk()).andReturn();
        JsonNode usuario = json(result);
        return new Login((MockHttpSession) result.getRequest().getSession(false), usuario.get("id").asLong());
    }

    private JsonNode criarMaterial(MockHttpSession sessao, String nome, int quantidade,
                                   String valorVenda, String largura, String comprimento) throws Exception {
        return json(mockMvc.perform(post("/api/materiais").session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(materialJson(nome, quantidade, valorVenda, largura, comprimento)))
                .andExpect(status().isOk()).andReturn());
    }

    private String materialJson(String nome, int quantidade, String valorVenda,
                                String largura, String comprimento) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.readTree("""
                {"nome":"%s","marca":"Marca Teste","cor":"Azul","quantidade":%d,
                 "estoqueMinimo":1,"valorCusto":1.00,"valorVenda":%s,
                 "larguraPeca":%s,"comprimentoPeca":%s,"unidadeMedida":"CM","areaPecaM2":999,
                 "descricao":"Material usado no teste de integração"}
                """.formatted(nome, quantidade, valorVenda, largura, comprimento)));
    }

    private String orcamentoJson(String cliente, String largura, String comprimento, long materialId) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.readTree("""
                {"nomeCliente":"%s","larguraObraM":%s,"comprimentoObraM":%s,"materialId":%d}
                """.formatted(cliente, largura, comprimento, materialId)));
    }

    private String obraJson(long orcamentoId, String nome, String endereco,
                            String descricao, String status) throws Exception {
        return objectMapper.writeValueAsString(objectMapper.readTree("""
                {"orcamentoId":%d,"nomeObra":"%s","endereco":"%s","descricao":"%s","status":"%s"}
                """.formatted(orcamentoId, nome, endereco, descricao, status)));
    }

    private String movimentoJson(long materialId, String tipo, int quantidade, String observacao) throws Exception {
        return objectMapper.writeValueAsString(new Movimento(materialId, null, tipo, quantidade, observacao));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private boolean contemOrcamento(JsonNode lista, long id, String cliente) {
        for (JsonNode item : lista) {
            if (item.get("id").asLong() == id && cliente.equals(item.get("nomeCliente").asText())) return true;
        }
        return false;
    }

    private JsonNode localizarOrcamento(JsonNode lista, long id) {
        for (JsonNode item : lista) {
            if (item.get("id").asLong() == id) return item;
        }
        throw new AssertionError("Orçamento " + id + " não encontrado.");
    }

    private record Credenciais(String login, String senha) {}
    private record Movimento(Long materialId, Long obraId, String tipo, Integer quantidade, String observacao) {}
    private record Login(MockHttpSession sessao, long usuarioId) {}
}
