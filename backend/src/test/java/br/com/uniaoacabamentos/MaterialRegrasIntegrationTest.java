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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MaterialRegrasIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void validaCamposConversoesDuplicidadeQuantidadeEInativacao() throws Exception {
        MockHttpSession admin = login();
        String base = "Material Regras " + UUID.randomUUID().toString().substring(0, 8);

        criarComErro(admin, jsonMaterial(base + " Sem Largura", null, 30, "CM", 0), "Largura");
        criarComErro(admin, jsonMaterial(base + " Sem Comprimento", 40, null, "CM", 0), "Comprimento");
        criarComErro(admin, jsonMaterial(base + " Sem Unidade", 40, 30, null, 0), "Unidade");
        criarComErro(admin, jsonMaterial(base + " Com Saldo", 40, 30, "CM", 5), "quantidade inicial");

        JsonNode mm = criar(admin, jsonMaterial(base + " MM", 400, 300, "MM", 0));
        criar(admin, jsonMaterial(base + " CM", 40, 30, "CM", 0));
        criar(admin, jsonMaterial(base + " M", 0.4, 0.3, "M", 0));

        mockMvc.perform(post("/api/materiais").session(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMaterial(base + " MM", 500, 500, "MM", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("mesmo nome, marca e cor")));

        String corDiferente = jsonMaterial(base + " MM", 500, 500, "MM", 0).replace("\"cor\":\"Cinza\"", "\"cor\":\"Preto\"");
        mockMvc.perform(post("/api/materiais").session(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(corDiferente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.areaPecaM2").value(0.25));

        long id = mm.get("id").asLong();
        mockMvc.perform(put("/api/materiais/{id}", id).session(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMaterial(base + " MM", 400, 300, "MM", 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("entradas e retiradas")));

        mockMvc.perform(delete("/api/materiais/{id}", id).session(admin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/materiais/{id}", id).session(admin)).andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/materiais/{id}/restaurar", id).session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void protegeValorDeCustoNaConsultaDoUsuarioComum() throws Exception {
        MockHttpSession admin = login("admin", "admin123");
        MockHttpSession usuario = login("usuario", "usuario123");
        String nome = "Material Consulta " + UUID.randomUUID().toString().substring(0, 8);
        long materialId = criar(admin, jsonMaterial(nome, 40, 30, "CM", 0)).get("id").asLong();

        mockMvc.perform(get("/api/materiais/{id}", materialId).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorCusto").value(1))
                .andExpect(jsonPath("$.valorVenda").value(2));

        mockMvc.perform(get("/api/materiais/{id}", materialId).session(usuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorCusto").doesNotExist())
                .andExpect(jsonPath("$.valorVenda").value(2));

        mockMvc.perform(get("/api/materiais").session(usuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].valorCusto").isEmpty());

        mockMvc.perform(delete("/api/materiais/{id}", materialId).session(admin))
                .andExpect(status().isOk());

        JsonNode inativos = json(mockMvc.perform(get("/api/materiais/excluidos").session(admin))
                .andExpect(status().isOk()).andReturn());
        boolean materialInativoEncontrado = false;
        for (JsonNode material : inativos) {
            if (material.path("id").asLong() == materialId
                    && !material.path("ativo").asBoolean(true)
                    && material.path("valorCusto").asDouble() == 1.0) {
                materialInativoEncontrado = true;
                break;
            }
        }
        assertTrue(materialInativoEncontrado,
                "O administrador deve conseguir consultar o material inativo com o valor de custo.");
    }

    private void criarComErro(MockHttpSession sessao, String json, String mensagem) throws Exception {
        mockMvc.perform(post("/api/materiais").session(sessao).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString(mensagem)));
    }

    private JsonNode criar(MockHttpSession sessao, String json) throws Exception {
        return json(mockMvc.perform(post("/api/materiais").session(sessao)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaPecaM2").value(0.12)).andReturn());
    }

    private String jsonMaterial(String nome, Number largura, Number comprimento,
                                String unidade, int quantidade) throws Exception {
        var node = objectMapper.createObjectNode();
        node.put("nome", nome); node.put("marca", "Marca Teste"); node.put("cor", "Cinza");
        node.put("descricao", "Material para validar regras"); node.put("quantidade", quantidade);
        node.put("estoqueMinimo", 1); node.put("valorCusto", 1); node.put("valorVenda", 2);
        if (largura != null) node.put("larguraPeca", largura.doubleValue());
        if (comprimento != null) node.put("comprimentoPeca", comprimento.doubleValue());
        if (unidade != null) node.put("unidadeMedida", unidade);
        return objectMapper.writeValueAsString(node);
    }

    private MockHttpSession login() throws Exception {
        return login("admin", "admin123");
    }

    private MockHttpSession login(String login, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Credenciais(login, senha))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private record Credenciais(String login, String senha) {}
}
