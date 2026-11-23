package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuadorELogsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void deveExporSomenteHealthSemAutenticacao() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/healthz"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void infoExigeAutenticacaoEMetricasExigemAdmin() throws Exception {
        String usuario = TestAutenticacao.cadastrarELogar(mockMvc);
        String admin = TestAutenticacao.cadastrarAdminELogar(mockMvc, usuarioRepository);

        mockMvc.perform(get("/actuator/info").header("Authorization", "Bearer " + usuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("desapegoUniversitario"));

        mockMvc.perform(get("/actuator/metrics").header("Authorization", "Bearer " + usuario))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/metrics").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    void devePropagarCorrelationIdELimparMdc() throws Exception {
        mockMvc.perform(get("/api/v1/anuncios").header("X-Correlation-Id", "corr-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "corr-123"));
        assertNull(MDC.get("correlationId"));

        String gerado = mockMvc.perform(get("/api/v1/anuncios"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("X-Correlation-Id");
        assertTrue(gerado != null && gerado.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void deveUsarLogsEstruturadosEmJson() {
        assertEquals("logstash", environment.getProperty("logging.structured.format.console"));
        assertEquals("true", environment.getProperty("logging.structured.json.context.include"));
    }
}
