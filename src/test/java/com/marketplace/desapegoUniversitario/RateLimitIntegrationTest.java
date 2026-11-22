package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.security.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.rate-limit.login=3",
        "app.rate-limit.cadastro=2",
        "app.rate-limit.janela=PT1M"
})
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void reiniciarContadores() {
        rateLimitFilter.limpar();
    }

    @Test
    void loginDeveSerLimitadoPorIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            tentarLogin();
        }

        mockMvc.perform(post("/api/v1/usuarios/login")
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("10.0.0.1");
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"aluno@universidade.edu.br","senha":"Senha@12345"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("LIMITE_EXCEDIDO"))
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void cadastroDeveSerLimitadoPorIp() throws Exception {
        for (int i = 0; i < 2; i++) {
            tentarCadastro();
        }

        mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("10.0.0.2");
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s@universidade.edu.br","senha":"Senha@12345"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isTooManyRequests());
    }

    private void tentarLogin() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/login")
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("10.0.0.1");
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ninguem@universidade.edu.br","senha":"Senha@12345"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private void tentarCadastro() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("10.0.0.2");
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s@universidade.edu.br","senha":"Senha@12345"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());
    }
}
