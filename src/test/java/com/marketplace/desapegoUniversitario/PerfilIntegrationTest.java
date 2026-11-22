package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.repository.AnuncioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PerfilIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnuncioRepository anuncioRepository;

    @Test
    void perfilDeveTrazerDadosBasicosSemSenhaOuHash() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        String email = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.nome").value("Aluno Teste"))
                        .andExpect(jsonPath("$.role").value("USUARIO"))
                        .andExpect(jsonPath("$.emailVerificado").value(true))
                        .andExpect(jsonPath("$.criadoEm").exists())
                        .andExpect(jsonPath("$.contato").value(org.hamcrest.Matchers.nullValue()))
                        .andExpect(jsonPath("$.avatar").value(org.hamcrest.Matchers.nullValue()))
                        .andReturn().getResponse().getContentAsString(), "$.email");

        org.junit.jupiter.api.Assertions.assertTrue(email.endsWith("@universidade.edu.br"));
        String corpo = mockMvc.perform(get("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        assertFalse(corpo.contains("senha"));
        assertFalse(corpo.contains("$2a$"));
    }

    @Test
    void perfilDevePermitirAtualizarNome() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);

        mockMvc.perform(patch("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Novo Nome\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo Nome"));

        mockMvc.perform(patch("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void perfilDevePermitirEditarELimparContatoEAvatar() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);

        mockMvc.perform(patch("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria Almeida\",\"contato\":\"WhatsApp 99999-9999\","
                                + "\"avatar\":\"/uploads/perfis/maria.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Almeida"))
                .andExpect(jsonPath("$.contato").value("WhatsApp 99999-9999"))
                .andExpect(jsonPath("$.avatar").value("/uploads/perfis/maria.png"));

        mockMvc.perform(patch("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contato\":\"Instagram @maria\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Almeida"))
                .andExpect(jsonPath("$.contato").value("Instagram @maria"));

        mockMvc.perform(patch("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contato\":\"\",\"avatar\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contato").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.avatar").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void perfilDeveRejeitarAvatarInseguroOuContatoLongo() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);

        mockMvc.perform(patch("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avatar\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[0].campo").value("avatar"));

        mockMvc.perform(patch("/api/v1/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contato\":\"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[0].campo").value("contato"));
    }

    @Test
    void historicoDeveSerDoUsuarioRespeitandoStatusERemocao() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        String outro = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID ativo = TestAutenticacao.criarAnuncio(mockMvc, dono);
        UUID reservado = TestAutenticacao.criarAnuncio(mockMvc, dono);
        UUID alheio = TestAutenticacao.criarAnuncio(mockMvc, outro);
        UUID removido = TestAutenticacao.criarAnuncio(mockMvc, dono);

        var anuncio = anuncioRepository.findById(reservado).orElseThrow();
        anuncio.setStatus(StatusAnuncio.RESERVADO);
        anuncioRepository.save(anuncio);

        mockMvc.perform(delete("/api/v1/anuncios/{id}", removido)
                        .header("Authorization", "Bearer " + dono))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/usuarios/me/anuncios")
                        .header("Authorization", "Bearer " + dono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.id == '" + alheio + "')]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.id == '" + removido + "')]").doesNotExist());

        mockMvc.perform(get("/api/v1/usuarios/me/anuncios")
                        .header("Authorization", "Bearer " + dono)
                        .param("incluirRemovidos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/v1/usuarios/me/anuncios")
                        .header("Authorization", "Bearer " + dono)
                        .param("status", "RESERVADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(reservado.toString()));

        org.junit.jupiter.api.Assertions.assertNotNull(ativo);
    }

    @Test
    void perfilEHistoricoDevemExigirAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/usuarios/me/anuncios"))
                .andExpect(status().isUnauthorized());
    }
}
