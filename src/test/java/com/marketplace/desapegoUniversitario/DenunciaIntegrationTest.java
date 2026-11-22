package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DenunciaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AnuncioRepository anuncioRepository;

    @Test
    void usuarioDeveDenunciarEAdminDeveResolverRemovendoAnuncio() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, dono);
        String denunciante = TestAutenticacao.cadastrarELogar(mockMvc);

        String corpo = mockMvc.perform(post("/api/v1/anuncios/{id}/denuncias", anuncioId)
                        .header("Authorization", "Bearer " + denunciante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Fraude\",\"descricao\":\"Preço suspeito\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.motivo").value("FRAUDE"))
                .andReturn().getResponse().getContentAsString();
        UUID denunciaId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(corpo, "$.id"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/denuncias", anuncioId)
                        .header("Authorization", "Bearer " + denunciante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Fraude\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/anuncios/{id}/denuncias", anuncioId)
                        .header("Authorization", "Bearer " + dono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Fraude\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));

        String admin = TestAutenticacao.cadastrarAdminELogar(mockMvc, usuarioRepository);
        mockMvc.perform(get("/api/v1/admin/denuncias")
                        .header("Authorization", "Bearer " + admin)
                        .param("status", "PENDENTE")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + denunciaId + "')]").exists());

        mockMvc.perform(post("/api/v1/admin/denuncias/{id}/resolucao", denunciaId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVIDA\",\"removerAnuncio\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDA"))
                .andExpect(jsonPath("$.resolvidaEm").exists());

        org.junit.jupiter.api.Assertions.assertTrue(
                anuncioRepository.findById(anuncioId).orElseThrow().getRemovidoEm() != null);

        mockMvc.perform(post("/api/v1/admin/denuncias/{id}/resolucao", denunciaId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJEITADA\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/admin/denuncias")
                        .header("Authorization", "Bearer " + admin)
                        .param("status", "RESOLVIDA")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + denunciaId + "')]").exists());
    }

    @Test
    void denunciasDevemExigirAutenticacaoEAdmin() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, token);

        mockMvc.perform(post("/api/v1/anuncios/{id}/denuncias", anuncioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Spam\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/denuncias")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/anuncios/{id}/denuncias", anuncioId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/denuncias", anuncioId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Sem motivo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[0].campo").value("motivo"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/denuncias", anuncioId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"INEXISTENTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/denuncias", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Spam\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminPodeRejeitarDenunciaSemRemoverAnuncio() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, dono);
        String denunciante = TestAutenticacao.cadastrarELogar(mockMvc);
        String admin = TestAutenticacao.cadastrarAdminELogar(mockMvc, usuarioRepository);

        String corpo = mockMvc.perform(post("/api/v1/anuncios/{id}/denuncias", anuncioId)
                        .header("Authorization", "Bearer " + denunciante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Spam\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID denunciaId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(corpo, "$.id"));

        mockMvc.perform(post("/api/v1/admin/denuncias/{id}/resolucao", denunciaId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJEITADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADA"));

        org.junit.jupiter.api.Assertions.assertNull(
                anuncioRepository.findById(anuncioId).orElseThrow().getRemovidoEm());
        mockMvc.perform(get("/api/v1/admin/denuncias")
                        .header("Authorization", "Bearer " + admin)
                        .param("status", "REJEITADA")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + denunciaId + "')]").exists());
    }

    @Test
    void denuncianteDeveAcompanharSuasDenunciasSemVerAsDosOutros() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, dono);
        String denunciante = TestAutenticacao.cadastrarELogar(mockMvc);
        String outro = TestAutenticacao.cadastrarELogar(mockMvc);
        String admin = TestAutenticacao.cadastrarAdminELogar(mockMvc, usuarioRepository);

        String corpo = mockMvc.perform(post("/api/v1/anuncios/{id}/denuncias", anuncioId)
                        .header("Authorization", "Bearer " + denunciante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"PRODUTO_PROIBIDO\",\"descricao\":\"Produto irregular\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.motivo").value("PRODUTO_PROIBIDO"))
                .andReturn().getResponse().getContentAsString();
        UUID denunciaId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(corpo, "$.id"));

        mockMvc.perform(get("/api/v1/usuarios/me/denuncias")
                        .header("Authorization", "Bearer " + denunciante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + denunciaId + "')]").exists())
                .andExpect(jsonPath("$.content[0].status").value("PENDENTE"));

        mockMvc.perform(get("/api/v1/usuarios/me/denuncias/{id}", denunciaId)
                        .header("Authorization", "Bearer " + denunciante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(denunciaId.toString()))
                .andExpect(jsonPath("$.motivo").value("PRODUTO_PROIBIDO"))
                .andExpect(jsonPath("$.descricao").value("Produto irregular"));

        mockMvc.perform(get("/api/v1/usuarios/me/denuncias/{id}", denunciaId)
                        .header("Authorization", "Bearer " + outro))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mockMvc.perform(get("/api/v1/usuarios/me/denuncias")
                        .header("Authorization", "Bearer " + outro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + denunciaId + "')]").doesNotExist());

        mockMvc.perform(post("/api/v1/admin/denuncias/{id}/resolucao", denunciaId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVIDA\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/usuarios/me/denuncias/{id}", denunciaId)
                        .header("Authorization", "Bearer " + denunciante))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVIDA"))
                .andExpect(jsonPath("$.resolvidaEm").exists());
    }
}
