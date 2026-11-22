package com.marketplace.desapegoUniversitario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratoApiIntegrationTest {

    private static final String UUID_REGEX =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final String ISO_UTC_REGEX =
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveUsarUuidDatasIsoUtcEEnumsCanonicos() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);

        mockMvc.perform(post("/api/v1/anuncios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Caderno","preco":12.50,"categoria":"livros",
                                 "tipo":"doacao","condicao":"novo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(matchesPattern(UUID_REGEX)))
                .andExpect(jsonPath("$.criadoEm").value(matchesPattern(ISO_UTC_REGEX)))
                .andExpect(jsonPath("$.categoria").value("LIVROS"))
                .andExpect(jsonPath("$.tipo").value("DOACAO"))
                .andExpect(jsonPath("$.condicao").value("NOVO"));
    }

    @Test
    void deveManterMesmoFormatoDePaginacao() throws Exception {
        mockMvc.perform(get("/api/v1/anuncios")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.first").isBoolean())
                .andExpect(jsonPath("$.last").isBoolean());

        mockMvc.perform(get("/api/v1/anuncios")
                        .param("page", "-1")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }

    @Test
    void deveTerMigradoContratosComFlywaySemPerderDados() throws Exception {
        Integer migracoes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '13' AND success = TRUE",
                Integer.class);
        assertEquals(1, migracoes);

        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        assertTrue(token.length() > 20);

        Integer usuarios = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuario", Integer.class);
        Integer rolesValidas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE role IN ('USUARIO', 'ADMIN')", Integer.class);
        assertEquals(usuarios, rolesValidas);
    }
}
