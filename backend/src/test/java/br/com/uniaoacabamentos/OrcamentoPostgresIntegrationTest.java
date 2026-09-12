package br.com.uniaoacabamentos;

import br.com.uniaoacabamentos.model.StatusUsuario;
import br.com.uniaoacabamentos.model.TipoUsuario;
import br.com.uniaoacabamentos.model.Usuario;
import br.com.uniaoacabamentos.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke test opcional contra o PostgreSQL configurado em application.properties.
 * As inclusões são revertidas ao final para não deixar dados artificiais no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfSystemProperty(named = "unicontrol.postgres.test", matches = "true")
class OrcamentoPostgresIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UsuarioRepository usuarioRepository;

    @Test
    void deveSalvarListarEContratarOrcamentoDeObraComVinculoInicialNulo() throws Exception {
        String permiteNulo = jdbcTemplate.queryForObject("""
                SELECT is_nullable
                  FROM information_schema.columns
                 WHERE table_schema = current_schema()
                   AND table_name = 'orcamento'
                   AND column_name = 'obra_id'
                """, String.class);
        assertThat(permiteNulo).isEqualTo("YES");

        MockHttpSession admin = sessaoAdministrador();
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        JsonNode material = json(mockMvc.perform(post("/api/materiais").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Piso laminado PostgreSQL %s","marca":"Teste PostgreSQL","cor":"Natural",
                                 "descricao":"Registro transitório do teste de orçamento","quantidade":0,
                                 "estoqueMinimo":0,"valorCusto":80,"valorVenda":119,
                                 "larguraPeca":19,"comprimentoPeca":135,"unidadeMedida":"CM"}
                                """.formatted(sufixo)))
                .andExpect(status().isOk()).andReturn());
        long materialId = material.get("id").asLong();
        String orcamentoPayload = """
                {"nomeCliente":"Cliente PostgreSQL %s","larguraObraM":10,
                 "comprimentoObraM":2,"materialId":%d}
                """.formatted(sufixo, materialId);

        JsonNode orcamento = json(mockMvc.perform(post("/api/orcamentos").session(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(orcamentoPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoOrcamento").value("OBRA"))
                .andExpect(jsonPath("$.status").value("CALCULADO"))
                .andExpect(jsonPath("$.obraId").isEmpty())
                .andExpect(jsonPath("$.areaObraM2").value(20.0))
                .andExpect(jsonPath("$.areaPecaM2").value(0.2565))
                .andExpect(jsonPath("$.quantidadePecas").value(78))
                .andExpect(jsonPath("$.valorTotal").value(9282.0))
                .andReturn());
        long orcamentoId = orcamento.get("id").asLong();

        JsonNode listado = localizar(json(mockMvc.perform(get("/api/orcamentos").session(admin))
                .andExpect(status().isOk()).andReturn()), orcamentoId);
        assertThat(listado.get("obraId").isNull()).isTrue();
        assertThat(listado.get("itens").get(0).get("subtotal").decimalValue())
                .isEqualByComparingTo("9282.00");

        JsonNode obra = json(mockMvc.perform(post("/api/obras").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orcamentoId":%d,"nomeObra":"Obra PostgreSQL %s",
                                 "endereco":"Rua PostgreSQL, 20","descricao":"Teste transacional real"}
                                """.formatted(orcamentoId, sufixo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CADASTRADA"))
                .andExpect(jsonPath("$.valorContratado").value(9282.0))
                .andExpect(jsonPath("$.orcamentoOrigem.status").value("CONTRATADO"))
                .andReturn());

        JsonNode contratado = localizar(json(mockMvc.perform(get("/api/orcamentos").session(admin))
                .andExpect(status().isOk()).andReturn()), orcamentoId);
        assertThat(contratado.get("status").asText()).isEqualTo("CONTRATADO");
        assertThat(contratado.get("obraId").asLong()).isEqualTo(obra.get("id").asLong());

        mockMvc.perform(get("/api/materiais/{id}", materialId).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(0));
    }

    private MockHttpSession sessaoAdministrador() {
        Usuario administrador = usuarioRepository.findAtivos(StatusUsuario.ATIVO).stream()
                .filter(usuario -> usuario.getTipoUsuario().normalizado() == TipoUsuario.ADMINISTRADOR)
                .findFirst()
                .orElseThrow(() -> new AssertionError("O banco precisa possuir um administrador ativo."));
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute("UNICONTROL_USUARIO_ID", administrador.getId());
        return sessao;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private JsonNode localizar(JsonNode lista, long id) {
        for (JsonNode item : lista) {
            if (item.get("id").asLong() == id) return item;
        }
        throw new AssertionError("Orçamento " + id + " não encontrado após a gravação.");
    }
}
