package br.com.uniaoacabamentos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsuarioIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void validaNomeLoginUnicoEdicaoEPasswordOculta() throws Exception {
        MockHttpSession admin = login("admin", "admin123").sessao();
        String login = "user_" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/usuarios").session(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson("   ", login, "senha123", "USUARIO")))
                .andExpect(status().isBadRequest());

        JsonNode criado = json(mockMvc.perform(post("/api/usuarios").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson("Usuário Teste", login, "senha123", "USUARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.caminhoFoto").isEmpty()).andReturn());

        mockMvc.perform(post("/api/usuarios").session(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson("Outro Usuário", login.toUpperCase(), "senha456", "USUARIO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Já existe um usuário cadastrado com este login."));

        mockMvc.perform(put("/api/usuarios/{id}", criado.get("id").asLong()).session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson("Usuário Editado", login, "", "USUARIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Usuário Editado"))
                .andExpect(jsonPath("$.login").value(login));
    }

    @Test
    void bloqueiaInativoEAtualizaUltimoAcessoSomenteNoSucesso() throws Exception {
        MockHttpSession admin = login("admin", "admin123").sessao();
        String login = "inactive_" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode criado = criarUsuario(admin, "Usuário Inativo", login, "senha123", "USUARIO");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(login, "incorreta")))
                .andExpect(status().isBadRequest());
        JsonNode antesDoSucesso = json(mockMvc.perform(get("/api/usuarios").session(admin))
                .andExpect(status().isOk()).andReturn());
        assertThat(localizarUsuario(antesDoSucesso, criado.get("id").asLong()).get("ultimoAcesso").isNull()).isTrue();

        Login sucesso = login(login, "senha123");
        mockMvc.perform(get("/api/auth/me").session(sucesso.sessao()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimoAcesso").isNotEmpty());

        mockMvc.perform(patch("/api/usuarios/{id}/status", criado.get("id").asLong())
                        .param("status", "INATIVO").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INATIVO"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(login, "senha123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Usuário inativo. Entre em contato com o administrador."));
    }

    @Test
    void protegeUltimoAdministradorAtivo() throws Exception {
        Login admin = login("admin", "admin123");

        mockMvc.perform(patch("/api/usuarios/{id}/status", admin.usuarioId())
                        .param("status", "INATIVO").session(admin.sessao()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/usuarios/{id}", admin.usuarioId()).session(admin.sessao())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson("Administrador", "admin", "", "USUARIO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("administrador ativo")));
    }

    @Test
    void adicionaSubstituiRemoveFotoERecusaArquivoFalso() throws Exception {
        MockHttpSession admin = login("admin", "admin123").sessao();
        String login = "foto_" + UUID.randomUUID().toString().substring(0, 8);
        long id = criarUsuario(admin, "Usuário Foto", login, "senha123", "USUARIO").get("id").asLong();

        MockMultipartFile falsa = new MockMultipartFile("foto", "foto.png", "image/png", "não é imagem".getBytes());
        mockMvc.perform(multipart("/api/usuarios/{id}/foto", id).file(falsa).session(admin)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("não é uma imagem")));

        byte[] png = imagemValida("png");
        MockMultipartFile valida = new MockMultipartFile("foto", "perfil.png", "application/octet-stream", png);
        JsonNode comFoto = json(mockMvc.perform(multipart("/api/usuarios/{id}/foto", id).file(valida).session(admin)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caminhoFoto").value(org.hamcrest.Matchers.endsWith(".png"))).andReturn());
        assertThat(comFoto.get("caminhoFoto").asText()).isNotBlank();

        byte[] jpeg = imagemValida("jpg");
        MockMultipartFile substituta = new MockMultipartFile("foto", "perfil.jpg", "image/jpeg", jpeg);
        JsonNode substituida = json(mockMvc.perform(multipart("/api/usuarios/{id}/foto", id).file(substituta).session(admin)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caminhoFoto").value(org.hamcrest.Matchers.endsWith(".jpg"))).andReturn());
        assertThat(substituida.get("caminhoFoto").asText()).isNotEqualTo(comFoto.get("caminhoFoto").asText());

        mockMvc.perform(get("/api/usuarios/{id}/foto", id).session(admin))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));

        mockMvc.perform(delete("/api/usuarios/{id}/foto", id).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caminhoFoto").isEmpty());
    }

    private JsonNode criarUsuario(MockHttpSession sessao, String nome, String login,
                                  String senha, String tipo) throws Exception {
        return json(mockMvc.perform(post("/api/usuarios").session(sessao)
                        .contentType(MediaType.APPLICATION_JSON).content(usuarioJson(nome, login, senha, tipo)))
                .andExpect(status().isOk()).andReturn());
    }

    private Login login(String login, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciais(login, senha))).andExpect(status().isOk()).andReturn();
        return new Login((MockHttpSession) result.getRequest().getSession(false), json(result).get("id").asLong());
    }

    private String usuarioJson(String nome, String login, String senha, String tipo) throws Exception {
        return objectMapper.writeValueAsString(new UsuarioPayload(nome, login, senha, tipo));
    }

    private String credenciais(String login, String senha) throws Exception {
        return objectMapper.writeValueAsString(new Credenciais(login, senha));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private JsonNode localizarUsuario(JsonNode lista, long id) {
        for (JsonNode usuario : lista) if (usuario.get("id").asLong() == id) return usuario;
        throw new AssertionError("Usuário de teste não encontrado.");
    }

    private byte[] imagemValida(String formato) throws Exception {
        BufferedImage imagem = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        imagem.setRGB(0, 0, Color.BLUE.getRGB());
        imagem.setRGB(1, 1, Color.WHITE.getRGB());
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(imagem, formato, saida);
        return saida.toByteArray();
    }

    private record UsuarioPayload(String nome, String login, String senha, String tipoUsuario) {}
    private record Credenciais(String login, String senha) {}
    private record Login(MockHttpSession sessao, long usuarioId) {}
}
