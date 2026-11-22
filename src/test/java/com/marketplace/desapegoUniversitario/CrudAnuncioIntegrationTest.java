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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrudAnuncioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnuncioRepository anuncioRepository;

    @Test
    void patchDeveAlterarSomenteOsCamposEnviados() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        String antes = mockMvc.perform(get("/api/v1/anuncios/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String precoAntes = com.jayway.jsonpath.JsonPath.read(antes, "$.preco").toString();
        String campusAntes = com.jayway.jsonpath.JsonPath.read(antes, "$.campus");

        mockMvc.perform(patch("/api/v1/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + dono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Livro revisado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Livro revisado"))
                .andExpect(jsonPath("$.preco").value(Double.parseDouble(precoAntes)))
                .andExpect(jsonPath("$.campus").value(campusAntes));
    }

    @Test
    void patchDeveRejeitarCampoInvalidoESomenteODonoPodeAlterar() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        String outro = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(patch("/api/v1/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + dono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + outro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Livro de outro\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void putEPatchDevemRejeitarAnuncioReservadoOuVendido() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        String comprador = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", id)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + dono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Reservado\",\"preco\":10.00,\"categoria\":\"LIVROS\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"));

        mockMvc.perform(patch("/api/v1/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + dono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Reservado revisado\",\"preco\":15.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/conclusao", id)
                        .header("Authorization", "Bearer " + dono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VENDIDO"));

        mockMvc.perform(patch("/api/v1/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + dono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Vendido\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"));
    }

    @Test
    void deleteDeveSerLogicoERemoverDosResultados() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(delete("/api/v1/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + dono))
                .andExpect(status().isNoContent());

        var registro = anuncioRepository.findById(id).orElseThrow();
        assertNotNull(registro.getRemovidoEm());
        assertEquals(StatusAnuncio.REMOVIDO, registro.getStatus());

        mockMvc.perform(get("/api/v1/anuncios/{id}", id))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/anuncios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + id + "')]").doesNotExist());
    }
}
