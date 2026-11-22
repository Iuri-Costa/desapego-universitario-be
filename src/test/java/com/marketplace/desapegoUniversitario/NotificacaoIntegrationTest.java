package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.domain.DispositivoFCM;
import com.marketplace.desapegoUniversitario.domain.Usuario;
import com.marketplace.desapegoUniversitario.repository.DispositivoFCMRepository;
import com.marketplace.desapegoUniversitario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificacaoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private DispositivoFCMRepository dispositivoRepository;

    @Test
    void mensagemDeveGerarNotificacaoParaODestinatario() throws Exception {
        String emailVendedor = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailVendedor);
        String vendedor = TestAutenticacao.login(mockMvc, emailVendedor);
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, vendedor);

        String emailComprador = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailComprador);
        String comprador = TestAutenticacao.login(mockMvc, emailComprador);

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/anuncios/{id}/mensagens", anuncioId)
                        .header("Authorization", "Bearer " + comprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\":\"Olá\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/notificacoes")
                        .header("Authorization", "Bearer " + vendedor)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.tipo == 'MENSAGEM')]").exists())
                .andExpect(jsonPath("$.content[?(@.referenciaId == '" + anuncioId + "')]").exists());

        mockMvc.perform(get("/api/v1/notificacoes/nao-lidas")
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/notificacoes")
                        .header("Authorization", "Bearer " + comprador)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.tipo == 'MENSAGEM')]").doesNotExist());
    }

    @Test
    void usuarioPodeMarcarNotificacaoComoLidaSomenteSeForDestinatario() throws Exception {
        String emailVendedor = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailVendedor);
        String vendedor = TestAutenticacao.login(mockMvc, emailVendedor);
        UUID anuncioId = TestAutenticacao.criarAnuncio(mockMvc, vendedor);

        String emailComprador = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, emailComprador);
        String comprador = TestAutenticacao.login(mockMvc, emailComprador);

        mockMvc.perform(post("/api/v1/anuncios/{id}/reserva", anuncioId)
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/anuncios/{id}/mensagens", anuncioId)
                        .header("Authorization", "Bearer " + comprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\":\"Olá\"}"))
                .andExpect(status().isCreated());

        String corpo = mockMvc.perform(get("/api/v1/notificacoes")
                        .header("Authorization", "Bearer " + vendedor)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> mensagens = com.jayway.jsonpath.JsonPath.read(
                corpo, "$.content[?(@.tipo == 'MENSAGEM')].id");
        String id = mensagens.get(0);
        String contagem = mockMvc.perform(get("/api/v1/notificacoes/nao-lidas")
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int naoLidasAntes = com.jayway.jsonpath.JsonPath.read(contagem, "$.total");

        mockMvc.perform(patch("/api/v1/notificacoes/{id}/lida", UUID.fromString(id))
                        .header("Authorization", "Bearer " + comprador))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/notificacoes/{id}/lida", UUID.fromString(id))
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lidaEm").exists());

        mockMvc.perform(patch("/api/v1/notificacoes/{id}/lida", UUID.fromString(id))
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notificacoes/nao-lidas")
                        .header("Authorization", "Bearer " + vendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(naoLidasAntes - 1));
    }

    @Test
    void dispositivoFCMDeveSerRegistradoERemovidoSemExporToken() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        String dispositivo = "token-fcm-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/notificacoes/dispositivo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + dispositivo + "\"}"))
                .andExpect(status().isCreated());

        String corpo = mockMvc.perform(get("/api/v1/notificacoes")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "100"))
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(corpo.contains(dispositivo));

        mockMvc.perform(post("/api/v1/notificacoes/dispositivo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));

        mockMvc.perform(delete("/api/v1/notificacoes/dispositivo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"  " + dispositivo + "  \"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/notificacoes/dispositivo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + dispositivo + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void dispositivoAntigoDeveSerRemovidoAoRegistrarNovoDispositivo() throws Exception {
        String email = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, email);
        String token = TestAutenticacao.login(mockMvc, email);
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        dispositivoRepository.save(new DispositivoFCM(null, usuario,
                "antigo-" + UUID.randomUUID(), Instant.now().minus(Duration.ofDays(120))));

        mockMvc.perform(post("/api/v1/notificacoes/dispositivo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"novo-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated());

        assertTrue(dispositivoRepository.findAll().stream()
                .noneMatch(dispositivo -> dispositivo.getToken().startsWith("antigo-")));
    }

    @Test
    void notificacoesDevemExigirAutenticacaoEValidarPaginacao() throws Exception {
        mockMvc.perform(get("/api/v1/notificacoes"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/notificacoes/nao-lidas"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/notificacoes/dispositivo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\"}"))
                .andExpect(status().isUnauthorized());

        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        mockMvc.perform(get("/api/v1/notificacoes")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));

        mockMvc.perform(get("/api/v1/notificacoes")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
}
