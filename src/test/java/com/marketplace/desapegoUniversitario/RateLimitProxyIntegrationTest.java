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
        "app.rate-limit.login=2",
        "app.rate-limit.janela=PT1M",
        "app.rate-limit.trusted-proxies=203\\.0\\.113\\.5|127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|0:0:0:0:0:0:0:1|::1"
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
    void proxyConfiavelDeveUsarIpDoClienteNoXForwardedFor() throws Exception {
        for (int i = 0; i < 2; i++) {
            login("198.51.100.10", "cliente-" + i + "@universidade.edu.br", "203.0.113.5");
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

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "198.51.100.11")
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("203.0.113.5");
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"Senha@12345"}
                                """.formatted(UUID.randomUUID() + "@universidade.edu.br")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void proxyNaoConfiavelNaoDevePermitirForjarXForwardedFor() throws Exception {
        for (int i = 0; i < 2; i++) {
            login("198.51.100.20", "forjado-" + i + "@universidade.edu.br", "198.51.100.99");
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "198.51.100.21")
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("198.51.100.99");
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"Senha@12345"}
                                """.formatted(UUID.randomUUID() + "@universidade.edu.br")))
                .andExpect(status().isTooManyRequests());
    }

    private void login(String ipCliente, String email, String remoteAddr) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", ipCliente)
                        .with(requisicao -> {
                            requisicao.setRemoteAddr(remoteAddr);
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"Senha@12345"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }
}
