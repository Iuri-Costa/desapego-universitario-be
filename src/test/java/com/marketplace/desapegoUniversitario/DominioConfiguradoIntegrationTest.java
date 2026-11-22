package com.marketplace.desapegoUniversitario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.cadastro.dominios-institucionais=universidade.test")
class DominioConfiguradoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveUsarListaDaConfiguracao() throws Exception {
        String aceito = "aluno." + UUID.randomUUID() + "@Universidade.test";
        mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s","senha":"%s"}
                                """.formatted(aceito, TestAutenticacao.SENHA)))
                .andExpect(status().isOk());

        String recusado = "aluno." + UUID.randomUUID() + "@universidade.edu.br";
        mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s","senha":"%s"}
                                """.formatted(recusado, TestAutenticacao.SENHA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DOMINIO_INVALIDO"));
    }
}
