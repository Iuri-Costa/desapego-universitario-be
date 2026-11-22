package com.marketplace.desapegoUniversitario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CacheEIndicesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveArmazenarBuscaPublicaNoCache() throws Exception {
        mockMvc.perform(get("/api/v1/anuncios").param("page", "0").param("size", "5"))
                .andExpect(status().isOk());

        CaffeineCache cache = (CaffeineCache) cacheManager.getCache("anuncios-busca");
        assertTrue(cache != null && cache.getNativeCache().estimatedSize() > 0);
    }

    @Test
    void alteracaoDeStatusPorReservaEConclusaoDeveInvalidarCacheDeBusca() throws Exception {
        String vendedor = TestAutenticacao.cadastrarELogar(mockMvc);
        String comprador = TestAutenticacao.cadastrarELogar(mockMvc);
        String titulo = "Cache " + UUID.randomUUID();
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, vendedor, titulo);

        mockMvc.perform(get("/api/v1/anuncios").param("q", titulo).param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ATIVO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVADO"));
        mockMvc.perform(get("/api/v1/anuncios").param("q", titulo).param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("RESERVADO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/conclusao", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VENDIDO"));
        mockMvc.perform(get("/api/v1/anuncios").param("q", titulo).param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("VENDIDO"));
    }

    @Test
    void deveCriarIndicesDeBuscaNaMigrationV14() {
        String produto = jdbcTemplate.execute((ConnectionCallback<String>) connection ->
                connection.getMetaData().getDatabaseProductName());
        String sql = "PostgreSQL".equalsIgnoreCase(produto)
                ? "SELECT indexname FROM pg_indexes WHERE tablename = 'anuncio' AND indexname LIKE 'idx_anuncio_%'"
                : "SELECT index_name FROM information_schema.indexes WHERE UPPER(table_name) = 'ANUNCIO' AND UPPER(index_name) LIKE 'IDX_ANUNCIO_%'";
        List<String> indices = jdbcTemplate.queryForList(sql, String.class);

        assertFalse(indices.isEmpty());
        assertTrue(indices.stream().anyMatch(i -> i.contains("CATEGORIA") || i.contains("categoria")));
        assertTrue(indices.stream().anyMatch(i -> i.contains("LISTAGEM") || i.contains("listagem")));
    }
}
