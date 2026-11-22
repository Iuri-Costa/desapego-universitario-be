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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MensagemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void compradorEVendedorDevemTrocarMensagensPeloAnuncio() throws Exception {
        String emailVendedor = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailVendedor);
        String vendedor = TestAutenticacao.login(mockMvc, emailVendedor);
        UUID vendedorId = usuarioRepository.findByEmail(emailVendedor).orElseThrow().getId();
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, vendedor);

        String emailComprador = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailComprador);
        String comprador = TestAutenticacao.login(mockMvc, emailComprador);
        UUID compradorId = usuarioRepository.findByEmail(emailComprador).orElseThrow().getId();

        String primeira = mockMvc.perform(post("/api/v1/anuncios/{id}/mensagens", anuncioId)
                        .header("Authorization", "Bearer " + comprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\":\"Ainda está disponível?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remetenteId").value(compradorId.toString()))
                .andExpect(jsonPath("$.destinatarioId").value(vendedorId.toString()))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/v1/anuncios/{id}/mensagens", anuncioId)
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinatarioId":"%s","conteudo":"Sim, disponível"}
                                """.formatted(compradorId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/anuncios/{id}/mensagens", anuncioId)
                        .header("Authorization", "Bearer " + vendedor)
                        .param("compradorId", compradorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].conteudo").value("Ainda está disponível?"))
                .andExpect(jsonPath("$[1].conteudo").value("Sim, disponível"));

        mockMvc.perform(get("/api/v1/anuncios/{id}/mensagens", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        UUID mensagemId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(primeira, "$.id"));
        mockMvc.perform(patch("/api/v1/mensagens/{id}/lida", mensagemId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lidaEm").exists());

        mockMvc.perform(patch("/api/v1/mensagens/{id}/lida", mensagemId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isForbidden());
    }

    @Test
    void terceiroNaoDeveLerConversaAlheia() throws Exception {
        String emailVendedor = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailVendedor);
        String vendedor = TestAutenticacao.login(mockMvc, emailVendedor);
        String emailComprador = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailComprador);
        String comprador = TestAutenticacao.login(mockMvc, emailComprador);
        String terceiro = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, vendedor);
        UUID compradorId = usuarioRepository.findByEmail(emailComprador).orElseThrow().getId();

        mockMvc.perform(get("/api/v1/anuncios/{id}/mensagens", anuncioId)
                        .header("Authorization", "Bearer " + terceiro)
                        .param("compradorId", compradorId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void mensagensDevemValidarParticipantesConteudoEAnuncio() throws Exception {
        String emailVendedor = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailVendedor);
        String vendedor = TestAutenticacao.login(mockMvc, emailVendedor);
        UUID vendedorId = usuarioRepository.findByEmail(emailVendedor).orElseThrow().getId();
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, vendedor);

        mockMvc.perform(post("/api/v1/anuncios/{id}/mensagens", anuncioId)
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatarioId\":\"" + vendedorId + "\",\"conteudo\":\"Oi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));

        mockMvc.perform(post("/api/v1/anuncios/{id}/mensagens", anuncioId)
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatarioId\":\"" + UUID.randomUUID() + "\",\"conteudo\":\"Oi\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/anuncios/{id}/mensagens", anuncioId)
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatarioId\":\"" + UUID.randomUUID() + "\",\"conteudo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));

        mockMvc.perform(delete("/api/v1/anuncios/{id}", anuncioId)
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/anuncios/{id}/mensagens", anuncioId)
                        .header("Authorization", "Bearer " + vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinatarioId\":\"" + UUID.randomUUID() + "\",\"conteudo\":\"Oi\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void mensagensDevemExigirAutenticacao() throws Exception {
        mockMvc.perform(post("/api/v1/anuncios/{id}/mensagens", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\":\"Oi\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/anuncios/{id}/mensagens", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
