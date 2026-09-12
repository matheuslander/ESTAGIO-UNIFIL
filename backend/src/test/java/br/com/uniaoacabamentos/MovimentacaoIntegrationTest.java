package br.com.uniaoacabamentos;

import br.com.uniaoacabamentos.model.Obra;
import br.com.uniaoacabamentos.model.StatusObra;
import br.com.uniaoacabamentos.repository.MovimentacaoEstoqueRepository;
import br.com.uniaoacabamentos.repository.ObraRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MovimentacaoIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ObraRepository obraRepository;
    @Autowired MovimentacaoEstoqueRepository movimentacaoRepository;

    @Test
    void aplicaPermissoesRecalculoAuditoriaEProtecaoDeSaldo() throws Exception {
        MockHttpSession admin = login("admin", "admin123");
        MockHttpSession usuario = login("usuario", "usuario123");
        long materialId = criarMaterial(admin);

        JsonNode entrada = movimentar(admin, materialId, "ENTRADA", 10, "Entrada administrativa");
        JsonNode retiradaAdmin = movimentar(admin, materialId, "SAIDA", 2, "Saída administrativa");
        mockMvc.perform(post("/api/materiais/movimentacoes").session(usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimentoJson(materialId, "ENTRADA", 1, "Entrada indevida")))
                .andExpect(status().isForbidden());

        JsonNode retiradaUsuario = movimentar(usuario, materialId, "SAIDA", 3, "Retirada própria");
        long retiradaId = retiradaUsuario.get("id").asLong();

        mockMvc.perform(put("/api/materiais/movimentacoes/{id}", retiradaAdmin.get("id").asLong()).session(usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicaoJson(1, "Tentativa de editar retirada alheia")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/materiais/movimentacoes/{id}", entrada.get("id").asLong()).session(usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicaoJson(9, "Tentativa de editar entrada")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/materiais/movimentacoes/{id}", retiradaId).session(usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicaoJson(4, "Retirada própria ajustada")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(4))
                .andExpect(jsonPath("$.dataUltimaAlteracao").isNotEmpty())
                .andExpect(jsonPath("$.usuarioUltimaAlteracao").doesNotExist());

        mockMvc.perform(get("/api/materiais/{id}", materialId).session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidade").value(4));

        mockMvc.perform(put("/api/materiais/movimentacoes/{id}", retiradaId).session(usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicaoJson(20, "Saldo ficaria negativo")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("Estoque insuficiente")));

        mockMvc.perform(get("/api/materiais/{id}", materialId).session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidade").value(4));

        mockMvc.perform(put("/api/materiais/movimentacoes/{id}", entrada.get("id").asLong()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicaoJson(8, "Entrada ajustada pelo administrador")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/materiais/{id}", materialId).session(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantidade").value(2));
    }

    @Test
    void permiteObrasAbertasBloqueiaEncerradasSemAlterarEstoqueEPreservaHistorico() throws Exception {
        MockHttpSession admin = login("admin", "admin123");
        long materialId = criarMaterial(admin);
        movimentar(admin, materialId, "ENTRADA", 20, "Entrada para validar obras");

        Obra cadastrada = criarObra(StatusObra.CADASTRADA);
        Obra emAndamento = criarObra(StatusObra.EM_ANDAMENTO);
        Obra pausada = criarObra(StatusObra.PAUSADA);

        JsonNode movimentoCadastrada = movimentar(admin, materialId, cadastrada.getId(), "SAIDA", 1,
                "Retirada em obra cadastrada");
        JsonNode movimentoEmAndamento = movimentar(admin, materialId, emAndamento.getId(), "SAIDA", 1,
                "Retirada em obra em andamento");
        movimentar(admin, materialId, pausada.getId(), "SAIDA", 1, "Retirada em obra pausada");

        mockMvc.perform(get("/api/materiais/{id}", materialId).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(17));

        cadastrada.setStatus(StatusObra.FINALIZADA);
        emAndamento.setStatus(StatusObra.CANCELADA);
        obraRepository.saveAndFlush(cadastrada);
        obraRepository.saveAndFlush(emAndamento);

        long movimentosAntesDosBloqueios = movimentacaoRepository.count();

        mockMvc.perform(post("/api/materiais/movimentacoes").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimentoJson(materialId, cadastrada.getId(), "SAIDA", 1,
                                "Tentativa em obra finalizada")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        "Não é possível registrar uma movimentação para esta obra porque ela está com o status FINALIZADA."));

        mockMvc.perform(post("/api/materiais/movimentacoes").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimentoJson(materialId, emAndamento.getId(), "SAIDA", 1,
                                "Tentativa em obra cancelada")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(
                        "Não é possível registrar uma movimentação para esta obra porque ela está com o status CANCELADA."));

        mockMvc.perform(get("/api/materiais/{id}", materialId).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(17));
        assertEquals(movimentosAntesDosBloqueios, movimentacaoRepository.count());

        MvcResult resultadoHistorico = mockMvc.perform(get("/api/materiais/movimentacoes").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].material").doesNotExist())
                .andExpect(jsonPath("$[0].usuario").doesNotExist())
                .andExpect(jsonPath("$[0].obra").doesNotExist())
                .andReturn();
        String jsonHistorico = resultadoHistorico.getResponse().getContentAsString();
        assertFalse(jsonHistorico.contains("ByteBuddyInterceptor"));
        assertFalse(jsonHistorico.contains("hibernateLazyInitializer"));
        JsonNode historico = json(resultadoHistorico);
        assertTrue(historicoContemMovimentoDaObra(historico, movimentoCadastrada.get("id").asLong(),
                StatusObra.FINALIZADA));
        assertTrue(historicoContemMovimentoDaObra(historico, movimentoEmAndamento.get("id").asLong(),
                StatusObra.CANCELADA));
    }

    private long criarMaterial(MockHttpSession admin) throws Exception {
        String nome = "Material Movimento " + UUID.randomUUID().toString().substring(0, 8);
        MvcResult result = mockMvc.perform(post("/api/materiais").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","marca":"Marca Teste","cor":"Cinza","descricao":"Material para movimentações",
                                "quantidade":0,"estoqueMinimo":1,"valorCusto":1,"valorVenda":2,
                                "larguraPeca":400,"comprimentoPeca":300,"unidadeMedida":"MM"}
                                """.formatted(nome)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areaPecaM2").value(0.12)).andReturn();
        return json(result).get("id").asLong();
    }

    private JsonNode movimentar(MockHttpSession sessao, long materialId, String tipo,
                                 int quantidade, String observacao) throws Exception {
        return movimentar(sessao, materialId, null, tipo, quantidade, observacao);
    }

    private JsonNode movimentar(MockHttpSession sessao, long materialId, Long obraId, String tipo,
                                 int quantidade, String observacao) throws Exception {
        return json(mockMvc.perform(post("/api/materiais/movimentacoes").session(sessao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movimentoJson(materialId, obraId, tipo, quantidade, observacao)))
                .andExpect(status().isOk()).andReturn());
    }

    private Obra criarObra(StatusObra status) {
        Obra obra = new Obra();
        String identificador = UUID.randomUUID().toString().substring(0, 8);
        obra.setNomeObra("Obra Movimento " + identificador);
        obra.setNomeCliente("Cliente Movimento " + identificador);
        obra.setEndereco("Endereço de teste " + identificador);
        obra.setDescricao("Obra criada para validar movimentações");
        obra.setMetragem(10.0);
        obra.setStatus(status);
        obra.setAtivo(true);
        return obraRepository.saveAndFlush(obra);
    }

    private boolean historicoContemMovimentoDaObra(JsonNode historico, long movimentoId, StatusObra statusAtual) {
        for (JsonNode movimento : historico) {
            if (movimento.path("id").asLong() == movimentoId
                    && movimento.path("obraStatus").asText().equals(statusAtual.name())) {
                return true;
            }
        }
        return false;
    }

    private MockHttpSession login(String login, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Credenciais(login, senha))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String movimentoJson(long materialId, String tipo, int quantidade, String observacao) throws Exception {
        return movimentoJson(materialId, null, tipo, quantidade, observacao);
    }

    private String movimentoJson(long materialId, Long obraId, String tipo, int quantidade,
                                  String observacao) throws Exception {
        return objectMapper.writeValueAsString(new Movimento(materialId, obraId, tipo, quantidade, observacao));
    }

    private String edicaoJson(int quantidade, String observacao) throws Exception {
        return objectMapper.writeValueAsString(new Edicao(quantidade, observacao));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private record Credenciais(String login, String senha) {}
    private record Movimento(Long materialId, Long obraId, String tipo, Integer quantidade, String observacao) {}
    private record Edicao(Integer quantidade, String observacao) {}
}
