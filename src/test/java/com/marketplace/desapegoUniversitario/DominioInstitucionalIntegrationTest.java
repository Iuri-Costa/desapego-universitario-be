package com.marketplace.desapegoUniversitario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DominioInstitucionalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void cadastroDeveAceitarDominiosInstitucionaisConfigurados() throws Exception {
        String email = "aluno." + UUID.randomUUID() + "@sub.universidade.edu.br";
        mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isOk());
    }

    @Test
    void cadastroDeveRecusarDominioPublico() throws Exception {
        String email = "aluno." + UUID.randomUUID() + "@gmail.com";
        mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DOMINIO_INVALIDO"))
                .andExpect(jsonPath("$.mensagem").value("Somente e-mails de domínio institucional são aceitos no cadastro"));
    }

    @Test
    void dominioQueNaoEInstitucionalNaoDeveSerAceitoPorSufixo() throws Exception {
        String email = "aluno." + UUID.randomUUID() + "@gmail.edu.br.falso";
        mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DOMINIO_INVALIDO"));
    }

}
