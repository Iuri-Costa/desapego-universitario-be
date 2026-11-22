package com.marketplace.desapegoUniversitario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProtecaoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void corsDeveAceitarSomenteOrigemConfigurada() throws Exception {
        mockMvc.perform(options("/api/v1/usuarios/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));

        mockMvc.perform(options("/api/v1/usuarios/login")
                        .header(HttpHeaders.ORIGIN, "https://site-malicioso.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void corsDevePermitirEExporCorrelationId() throws Exception {
        mockMvc.perform(options("/api/v1/usuarios/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-Correlation-Id"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        org.hamcrest.Matchers.containsString("X-Correlation-Id")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        org.hamcrest.Matchers.containsString("X-Correlation-Id")));
    }

    @Test
    void errosDevemIncluirCorrelationId() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/me")
                        .header("X-Correlation-Id", "correlacao-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.correlationId").value("correlacao-123"))
                .andExpect(header().string("X-Correlation-Id", "correlacao-123"));
    }

    @Test
    void respostasDevemConterHeadersDeSeguranca() throws Exception {
        mockMvc.perform(get("/api/v1/anuncios"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Permissions-Policy",
                        org.hamcrest.Matchers.containsString("geolocation=()")));
    }

    @Test
    void cadastroNaoDeveRevelarEmailJaCadastrado() throws Exception {
        String email = TestAutenticacao.novoEmail();
        String novo = mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String repetido = mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outro","email":"%s","senha":"Outra@Senha123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(novo, repetido);
    }

    @Test
    void loginNaoDeveRevelarSeEmailExiste() throws Exception {
        String inexistente = mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ninguem-%s@universidade.edu.br","senha":"Senha@12345"}
                                """.formatted(java.util.UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String email = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrarSemVerificar(mockMvc, email);
        String senhaErrada = mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"Errada@12345"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String mensagemInexistente = com.jayway.jsonpath.JsonPath.read(inexistente, "$.mensagem");
        String mensagemSenhaErrada = com.jayway.jsonpath.JsonPath.read(senhaErrada, "$.mensagem");
        org.junit.jupiter.api.Assertions.assertEquals(mensagemInexistente, mensagemSenhaErrada);
        org.junit.jupiter.api.Assertions.assertFalse(inexistente.contains(email));
    }

    @Test
    void cadastroNaoDeveExporSenhaNaResposta() throws Exception {
        String email = TestAutenticacao.novoEmail();
        String corpo = mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").exists())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(corpo.contains("senha"));
    }
}
