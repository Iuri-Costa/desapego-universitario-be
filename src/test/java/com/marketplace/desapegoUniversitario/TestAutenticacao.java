package com.marketplace.desapegoUniversitario;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestAutenticacao {

    public static final String SENHA = "Senha@12345";

    private TestAutenticacao() {
    }

    public static String novoEmail() {
        return "aluno-" + UUID.randomUUID() + "@universidade.edu.br";
    }

    public static void cadastrar(MockMvc mockMvc, String email) throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno Teste","email":"%s","senha":"%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk());
    }

    public static String login(MockMvc mockMvc, String email) throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.accessToken");
    }

    public static String refreshToken(MockMvc mockMvc, String email) throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.refreshToken");
    }

    public static String cadastrarELogar(MockMvc mockMvc) throws Exception {
        String email = novoEmail();
        cadastrar(mockMvc, email);
        return login(mockMvc, email);
    }
}
