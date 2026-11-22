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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
