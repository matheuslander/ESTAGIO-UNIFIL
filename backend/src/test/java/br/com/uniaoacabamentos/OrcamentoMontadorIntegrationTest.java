package br.com.uniaoacabamentos;

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
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrcamentoMontadorIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void calculaMantemUnicoENaoContratadoNaoAlteraObra() throws Exception {
        MockHttpSession admin = login("admin", "admin123");
        MockHttpSession usuario = login("usuario", "usuario123");
        long materialId = criarMaterial(admin);
        JsonNode obra = criarObra(admin, materialId, "Cliente Montador A", "Obra Montador A", 10, 10);
        long obraId = obra.get("id").asLong();

        String payload = montadorJson(obraId, "Montador José", 3);
        mockMvc.perform(post("/api/orcamentos/montador/calcular").session(usuario)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoOrcamento").value("MONTADOR"))
                .andExpect(jsonPath("$.metragemReferenciaM2").value(100.0))
                .andExpect(jsonPath("$.valorTotal").value(300.0))
                .andExpect(jsonPath("$.itens").isEmpty());

        JsonNode orcamento = json(mockMvc.perform(post("/api/orcamentos/montador").session(usuario)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CALCULADO"))
                .andExpect(jsonPath("$.obraId").value(obraId)).andReturn());

        mockMvc.perform(post("/api/orcamentos/montador").session(usuario)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Esta obra já possui um orçamento de montador."));

        mockMvc.perform(patch("/api/orcamentos/{id}/status", orcamento.get("id").asLong()).session(usuario)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"NAO_CONTRATADO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NAO_CONTRATADO"));

        mockMvc.perform(get("/api/obras/{id}", obraId).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeMontador").isEmpty())
                .andExpect(jsonPath("$.valorMontador").isEmpty());
    }

    @Test
    void contratarCopiaDadosPreservaEstoqueEValidaTransicoesDaObra() throws Exception {
        MockHttpSession admin = login("admin", "admin123");
        MockHttpSession usuario = login("usuario", "usuario123");
        long materialId = criarMaterial(admin);
        JsonNode obra = criarObra(admin, materialId, "Cliente Montador B", "Obra Montador B", 8, 5);
        long obraId = obra.get("id").asLong();

        JsonNode montador = json(mockMvc.perform(post("/api/orcamentos/montador").session(usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(montadorJson(obraId, "Montador Matheus", 4.5)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.valorTotal").value(180.0)).andReturn());

        mockMvc.perform(patch("/api/orcamentos/{id}/status", montador.get("id").asLong()).session(usuario)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONTRATADO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONTRATADO"));

        mockMvc.perform(get("/api/obras/{id}", obraId).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeMontador").value("Montador Matheus"))
                .andExpect(jsonPath("$.valorMontador").value(180.0))
                .andExpect(jsonPath("$.valorContratado").isNumber());

        mockMvc.perform(put("/api/orcamentos/montador/{id}", montador.get("id").asLong()).session(usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(montadorJson(obraId, "Montador Alterado", 5)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/materiais/{id}", materialId).session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidade").value(0));

        mockMvc.perform(patch("/api/obras/{id}/status", obraId).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"FINALIZADA\"}"))
                .andExpect(status().isBadRequest());
        alterarStatusObra(admin, obraId, "EM_ANDAMENTO");
        alterarStatusObra(admin, obraId, "PAUSADA");
        alterarStatusObra(admin, obraId, "EM_ANDAMENTO");
        mockMvc.perform(patch("/api/obras/{id}/status", obraId).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"FINALIZADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataFinalizacao").isNotEmpty());
    }

    private void alterarStatusObra(MockHttpSession admin, long id, String status) throws Exception {
        mockMvc.perform(patch("/api/obras/{id}/status", id).session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(status));
    }

    private long criarMaterial(MockHttpSession admin) throws Exception {
        String nome = "Material Orçamento " + UUID.randomUUID().toString().substring(0, 8);
        MvcResult result = mockMvc.perform(post("/api/materiais").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nome":"%s","marca":"Marca Montador","cor":"Branco","descricao":"Material orçamento",
                                "quantidade":0,"estoqueMinimo":0,"valorCusto":2,"valorVenda":5,
                                "larguraPeca":40,"comprimentoPeca":30,"unidadeMedida":"CM"}
                                """.formatted(nome)))
                .andExpect(status().isOk()).andReturn();
        return json(result).get("id").asLong();
    }

    private JsonNode criarObra(MockHttpSession admin, long materialId, String cliente,
                               String nomeObra, int largura, int comprimento) throws Exception {
        JsonNode orcamento = json(mockMvc.perform(post("/api/orcamentos").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeCliente\":\"" + cliente + "\",\"larguraObraM\":" + largura
                                + ",\"comprimentoObraM\":" + comprimento + ",\"materialId\":" + materialId + "}"))
                .andExpect(status().isCreated()).andReturn());
        return json(mockMvc.perform(post("/api/obras").session(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orcamentoId\":" + orcamento.get("id").asLong() + ",\"nomeObra\":\"" + nomeObra
                                + "\",\"endereco\":\"Rua dos Testes\",\"descricao\":\"Obra para montador\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CADASTRADA"))
                .andExpect(jsonPath("$.nomeCliente").value(cliente))
                .andExpect(jsonPath("$.valorContratado").isNumber()).andReturn());
    }

    private String montadorJson(long obraId, String nome, double valor) {
        return "{\"obraId\":" + obraId + ",\"nomeMontador\":\"" + nome
                + "\",\"valorPorMetroQuadrado\":" + valor + "}";
    }

    private MockHttpSession login(String login, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
