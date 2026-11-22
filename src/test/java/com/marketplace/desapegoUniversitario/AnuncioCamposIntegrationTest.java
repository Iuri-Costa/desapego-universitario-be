package com.marketplace.desapegoUniversitario;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnuncioCamposIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devePersistirTodosOsCamposDoAnuncio() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        String corpo = mockMvc.perform(post("/api/v1/anuncios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo":"Bicicleta",
                                  "descricao":"Em bom estado",
                                  "preco":250.00,
                                  "tipo":"VENDA",
                                  "categoria":"OUTROS",
                                  "condicao":"SEMINOVO",
                                  "status":"ATIVO",
                                  "campus":"CAMPUS-2",
                                  "localRetirada":"Biblioteca"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("VENDA"))
                .andExpect(jsonPath("$.condicao").value("SEMINOVO"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.campus").value("CAMPUS-2"))
                .andExpect(jsonPath("$.localRetirada").value("Biblioteca"))
                .andExpect(jsonPath("$.criadoEm").exists())
                .andExpect(jsonPath("$.atualizadoEm").exists())
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(corpo, "$.id");
        mockMvc.perform(get("/api/v1/anuncios/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Bicicleta"))
                .andExpect(jsonPath("$.campus").value("CAMPUS-2"));
    }

    @Test
    void deveAplicarPadroesParaCamposOpcionais() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        mockMvc.perform(post("/api/v1/anuncios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Caderno","preco":10.00,"categoria":"PAPELARIA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("VENDA"))
                .andExpect(jsonPath("$.condicao").value("USADO"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.campus").value("PRINCIPAL"))
                .andExpect(jsonPath("$.localRetirada").value("A combinar"));
    }

    @Test
    void deveRejeitarEnumeracoesInvalidas() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        mockMvc.perform(post("/api/v1/anuncios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Caderno","preco":10.00,"categoria":"PAPELARIA","tipo":"TROCA"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }
}
