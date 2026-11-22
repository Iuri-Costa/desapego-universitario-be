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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NegociacaoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void reservaConclusaoEAvaliacaoDevemFuncionar() throws Exception {
        String emailVendedor = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailVendedor);
        String vendedor = TestAutenticacao.login(mockMvc, emailVendedor);
        UUID vendedorId = usuarioRepository.findByEmail(emailVendedor).orElseThrow().getId();
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, vendedor);

        String emailComprador = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailComprador);
        String comprador = TestAutenticacao.login(mockMvc, emailComprador);
        UUID compradorId = usuarioRepository.findByEmail(emailComprador).orElseThrow().getId();

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVADO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());

        String outroEmail = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, outroEmail);
        String outro = TestAutenticacao.login(mockMvc, outroEmail);
        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + outro))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/conclusao", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/anuncios/{id}/conclusao", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VENDIDO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/avaliacoes", anuncioId)
                        .header("Authorization", "Bearer " + comprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":5,\"comentario\":\"Ótimo vendedor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value(5))
                .andExpect(jsonPath("$.vendedorId").value(vendedorId.toString()));

        mockMvc.perform(post("/api/v1/anuncios/{id}/avaliacoes", anuncioId)
                        .header("Authorization", "Bearer " + comprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nota").value(2));

        mockMvc.perform(get("/api/v1/usuarios/{id}/avaliacoes", vendedorId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.media").value(2.0));

        mockMvc.perform(post("/api/v1/anuncios/{id}/avaliacoes", anuncioId)
                        .header("Authorization", "Bearer " + outro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":5}"))
                .andExpect(status().isConflict());

        org.junit.jupiter.api.Assertions.assertNotNull(compradorId);
    }

    @Test
    void cancelamentoDeReservaDeveVoltarAnuncioParaAtivo() throws Exception {
        String vendedor = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, vendedor);
        String comprador = TestAutenticacao.cadastrarELogar(mockMvc);

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());

        String terceiro = TestAutenticacao.cadastrarELogar(mockMvc);
        mockMvc.perform(delete("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + terceiro))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));

        mockMvc.perform(delete("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isConflict());
    }

    @Test
    void doacaoConcluidaDeveVirarDoadoEValidarDados() throws Exception {
        String emailVendedor = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailVendedor);
        String vendedor = TestAutenticacao.login(mockMvc, emailVendedor);
        String emailComprador = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailComprador);
        String comprador = TestAutenticacao.login(mockMvc, emailComprador);

        String corpo = mockMvc.perform(post("/api/v1/anuncios")
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Mochila\",\"preco\":0,\"categoria\":\"OUTROS\",\"tipo\":\"DOACAO\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID anuncioId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(corpo, "$.id"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/anuncios/{id}/conclusao", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOADO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/avaliacoes", anuncioId)
                        .header("Authorization", "Bearer " + comprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    void negociacaoDeveExigirAutenticacaoEAnuncioExistente() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", id))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/usuarios/{id}/avaliacoes", id))
                .andExpect(status().isUnauthorized());

        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        mockMvc.perform(get("/api/v1/usuarios/{id}/avaliacoes", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }
}
