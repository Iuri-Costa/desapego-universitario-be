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
            tentarLogin("/api/v1/usuarios/login", "10.0.0.1", "ninguem-" + i + "@universidade.edu.br");
        }

        mockMvc.perform(post("/api/v1/usuarios/login")
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("10.0.0.1");
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"outro@universidade.edu.br","senha":"Senha@12345"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.codigo").value("LIMITE_EXCEDIDO"))
                .andExpect(jsonPath("$.caminho").value("/api/v1/usuarios/login"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void cadastroDeveSerLimitadoPorIp() throws Exception {
        for (int i = 0; i < 2; i++) {
            tentarCadastro("10.0.0.2", UUID.randomUUID() + "@universidade.edu.br");
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

    @Test
    void authLoginDeveSerLimitadoPorIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            tentarLogin("/api/v1/auth/login", "10.0.0.3", "ninguem-" + i + "@universidade.edu.br");
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("10.0.0.3");
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"outro@universidade.edu.br","senha":"Senha@12345"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("LIMITE_EXCEDIDO"))
                .andExpect(jsonPath("$.caminho").value("/api/v1/auth/login"))
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void loginDeveSerLimitadoPorEmailMesmoComIpsDiferentes() throws Exception {
        String email = "alvo-" + UUID.randomUUID() + "@universidade.edu.br";
        for (int i = 0; i < 3; i++) {
            tentarLogin("/api/v1/auth/login", "10.1.0." + i, email);
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("10.1.0.9");
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"Senha@12345"}
                                """.formatted(email)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("LIMITE_EXCEDIDO"))
                .andExpect(jsonPath("$.caminho").value("/api/v1/auth/login"));
    }

    @Test
    void cadastroDeveSerLimitadoPorEmailMesmoComIpsDiferentes() throws Exception {
        String email = UUID.randomUUID() + "@universidade.edu.br";
        for (int i = 0; i < 2; i++) {
            tentarCadastro("10.2.0." + i, email);
        }

        mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .with(requisicao -> {
                            requisicao.setRemoteAddr("10.2.0.9");
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s","senha":"Senha@12345"}
                                """.formatted(email)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("LIMITE_EXCEDIDO"));
    }

    private void tentarLogin(String rota, String ip, String email) throws Exception {
        mockMvc.perform(post(rota)
                        .with(requisicao -> {
                            requisicao.setRemoteAddr(ip);
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"Senha@12345"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    private void tentarCadastro(String ip, String email) throws Exception {
        mockMvc.perform(post("/api/v1/usuarios/cadastro")
                        .with(requisicao -> {
                            requisicao.setRemoteAddr(ip);
                            return requisicao;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Aluno","email":"%s","senha":"Senha@12345"}
                                """.formatted(email)))
                .andExpect(status().isOk());
    }
}
