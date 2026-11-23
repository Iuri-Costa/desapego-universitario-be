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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "server.forward-headers-strategy=framework",
        "app.rate-limit.login=2",
        "app.rate-limit.janela=PT1M"
})
class RateLimitProxyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void reiniciarContadores() {
        rateLimitFilter.limpar();
    }

    @Test
    void strategyFrameworkDeveUsarIpDoClienteNoXForwardedFor() throws Exception {
        for (int i = 0; i < 2; i++) {
            login("198.51.100.10", "cliente-" + i + "@universidade.edu.br");
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "198.51.100.10")
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("203.0.113.5");
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"Senha@12345"}
                                """.formatted(UUID.randomUUID() + "@universidade.edu.br")))
                .andExpect(status().isTooManyRequests());
    }

    private void login(String ipCliente, String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", ipCliente)
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("203.0.113.5");
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"Senha@12345"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }
}
