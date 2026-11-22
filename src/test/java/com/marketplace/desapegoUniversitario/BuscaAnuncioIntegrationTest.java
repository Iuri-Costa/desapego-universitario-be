package com.marketplace.desapegoUniversitario;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BuscaAnuncioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;
    private String marca;

    @BeforeEach
    void preparar() throws Exception {
        token = TestAutenticacao.cadastrarELogar(mockMvc);
        marca = "marca-" + java.util.UUID.randomUUID();
        criar(marca + " Bicicleta mountain", "Bicicleta para trilha", 100.00,
                "VENDA", "LIVROS", "NOVO", "CAMPUS-A");
        criar(marca + " Bicicleta usada", "Outra bicicleta", 250.00,
                "VENDA", "OUTROS", "USADO", "CAMPUS-B");
        criar(marca + " Mesa de estudo", "Mesa para o apartamento codigo-" + marca, 50.00,
                "DOACAO", "MOVEIS", "SEMINOVO", "CAMPUS-A");
    }

    @Test
    void filtrosDevemSerCombinaveis() throws Exception {
        mockMvc.perform(get("/api/v1/anuncios")
                        .param("q", marca)
                        .param("categoria", "LIVROS")
                        .param("tipo", "VENDA")
                        .param("precoMin", "90")
                        .param("precoMax", "110")
                        .param("condicao", "NOVO")
                        .param("campus", "campus-a")
                        .param("status", "ATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value(marca + " Bicicleta mountain"));
    }

    @Test
    void filtroDeTextoDeveBuscarTituloEDescricao() throws Exception {
        mockMvc.perform(get("/api/v1/anuncios").param("q", "codigo-" + marca))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value(marca + " Mesa de estudo"));

        mockMvc.perform(get("/api/v1/anuncios").param("q", marca.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void paginacaoEOrdenacaoDevemSerPadronizadas() throws Exception {
        MvcResult primeira = mockMvc.perform(get("/api/v1/anuncios")
                        .param("q", marca)
                        .param("page", "0").param("size", "1").param("sort", "preco,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content[0].preco").value(50.00))
                .andReturn();

        String ultimoId = JsonPath.read(primeira.getResponse().getContentAsString(), "$.content[0].id");

        mockMvc.perform(get("/api/v1/anuncios")
                        .param("q", marca)
                        .param("page", "1").param("size", "1").param("sort", "preco,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content[0].preco").value(100.00));

        mockMvc.perform(get("/api/v1/anuncios").param("q", marca).param("sort", "preco,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].preco").value(250.00));

        org.junit.jupiter.api.Assertions.assertNotNull(ultimoId);
    }

    @Test
    void parametrosInvalidosDevemRetornarBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/anuncios").param("precoMin", "200").param("precoMax", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));

        mockMvc.perform(get("/api/v1/anuncios").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));

        mockMvc.perform(get("/api/v1/anuncios").param("page", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/anuncios").param("sort", "senha,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));

        mockMvc.perform(get("/api/v1/anuncios").param("tipo", "TROCA"))
                .andExpect(status().isBadRequest());
    }

    private void criar(String titulo, String descricao, double preco,
                       String tipo, String categoria, String condicao, String campus) throws Exception {
        mockMvc.perform(post("/api/v1/anuncios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo":"%s",
                                  "descricao":"%s",
                                  "preco":%s,
                                  "tipo":"%s",
                                  "categoria":"%s",
                                  "condicao":"%s",
                                  "campus":"%s"
                                }
                                """.formatted(titulo, descricao, preco, tipo, categoria, condicao, campus)))
                .andExpect(status().isOk());
    }
}
