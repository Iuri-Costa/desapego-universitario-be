package com.marketplace.desapegoUniversitario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavoritoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void usuarioDeveAdicionarListarERemoverFavoritos() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, token);

        mockMvc.perform(post("/api/v1/anuncios/{id}/favoritos", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anuncioId").value(id.toString()))
                .andExpect(jsonPath("$.favorito").value(true));

        mockMvc.perform(post("/api/v1/anuncios/{id}/favoritos", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/usuarios/me/favoritos")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.id == '" + id + "')]").exists());

        mockMvc.perform(delete("/api/v1/anuncios/{id}/favoritos", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/usuarios/me/favoritos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + id + "')]").doesNotExist());
    }

    @Test
    void favoritosNaoDevemSerCompartilhadosEntreUsuarios() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        String outro = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(post("/api/v1/anuncios/{id}/favoritos", id)
                        .header("Authorization", "Bearer " + dono))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/usuarios/me/favoritos")
                        .header("Authorization", "Bearer " + outro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + id + "')]").doesNotExist());
    }

    @Test
    void favoritarAnuncioInexistenteDeveRetornarNotFound() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        mockMvc.perform(post("/api/v1/anuncios/{id}/favoritos", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void favoritosDevemExigirAutenticacao() throws Exception {
        mockMvc.perform(post("/api/v1/anuncios/{id}/favoritos", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/usuarios/me/favoritos"))
                .andExpect(status().isUnauthorized());
    }
}
