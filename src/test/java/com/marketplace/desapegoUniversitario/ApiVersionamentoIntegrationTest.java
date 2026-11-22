package com.marketplace.desapegoUniversitario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiVersionamentoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveCriarAnuncioNaVersaoV1() throws Exception {
        mockMvc.perform(post("/api/v1/anuncios")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION,
                                "Bearer " + TestAutenticacao.cadastrarELogar(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Livro de Cálculo",
                                  "preco": 50.00,
                                  "descricao": "Usado, em bom estado",
                                  "categoria": "LIVROS",
                                  "contato": "aluno@universidade.edu.br"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.titulo").value("Livro de Cálculo"));
    }

    @Test
    void deveListarAnunciosNaVersaoV1() throws Exception {
        mockMvc.perform(get("/api/v1/anuncios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void deveRetornarNaoEncontradoParaRotaSemVersao() throws Exception {
        mockMvc.perform(get("/anuncios")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION,
                                "Bearer " + TestAutenticacao.cadastrarELogar(mockMvc)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }
}
