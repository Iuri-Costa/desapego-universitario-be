package com.marketplace.desapegoUniversitario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProtecaoRotasLeituraIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listarImagensSemTokenDeveRetornar401() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(get("/api/v1/anuncios/{id}/imagens", id))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void consultarReservaSemTokenDeveRetornar401() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(get("/api/v1/anuncios/{id}/reserva", id))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void leituraDeImagensEReservaComTokenNaoDeveExigirDonoDaImagem() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(get("/api/v1/anuncios/{id}/imagens", id)
                        .header("Authorization", "Bearer " + dono))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/anuncios/{id}/reserva", id)
                        .header("Authorization", "Bearer " + dono))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void listagemDetalheECategoriaDevemContinuarPublicos() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(get("/api/v1/anuncios")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/anuncios/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/anuncios/categoria/{categoria}", "LIVROS"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/uploads/nada.png")).andExpect(status().isNotFound());
    }

}
