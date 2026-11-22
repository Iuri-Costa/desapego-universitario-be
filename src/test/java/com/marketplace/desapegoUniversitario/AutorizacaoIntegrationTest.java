package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutorizacaoIntegrationTest {

    private static final String ANUNCIO = """
            {"titulo":"Livro alterado","preco":20.00,"categoria":"LIVROS"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void donoDevePoderEditarERemoverSeuAnuncio() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(put("/api/v1/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + dono)
                        .contentType(MediaType.APPLICATION_JSON).content(ANUNCIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Livro alterado"));

        mockMvc.perform(delete("/api/v1/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + dono))
                .andExpect(status().isNoContent());
    }

    @Test
    void outroUsuarioNaoDeveEditarNemRemoverAnuncioAlheio() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        String outro = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(put("/api/v1/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + outro)
                        .contentType(MediaType.APPLICATION_JSON).content(ANUNCIO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mockMvc.perform(delete("/api/v1/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + outro))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminNaoDeveEditarAnuncioPelaRotaDeDono() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        String admin = TestAutenticacao.cadastrarAdminELogar(mockMvc, usuarioRepository);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(put("/api/v1/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(ANUNCIO))
                .andExpect(status().isForbidden());
    }

    @Test
    void somenteAdminDeveModerarAnuncios() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        String admin = TestAutenticacao.cadastrarAdminELogar(mockMvc, usuarioRepository);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(patch("/api/v1/admin/anuncios/{id}/moderacao", id)
                        .header("Authorization", "Bearer " + dono)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"moderado\":true}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/admin/anuncios/{id}/moderacao", id)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"moderado\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moderado").value(true));

        mockMvc.perform(delete("/api/v1/admin/anuncios/{id}", id)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void rotasDeAlteracaoDevemExigirAutenticacao() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(put("/api/v1/anuncios/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(ANUNCIO))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/admin/anuncios/{id}", id))
                .andExpect(status().isUnauthorized());
    }
}
