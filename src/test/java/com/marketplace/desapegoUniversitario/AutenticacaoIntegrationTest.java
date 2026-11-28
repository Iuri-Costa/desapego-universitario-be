package com.marketplace.desapegoUniversitario;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutenticacaoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginDeveRetornarParDeTokensSemExporSenhaOuHash() throws Exception {
        String email = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, email);

        String corpo = mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(corpo.contains("senha"));
        org.junit.jupiter.api.Assertions.assertFalse(corpo.contains("$2a$"));
    }

    @Test
    void loginLegadoDeveAvisarDeprecacaoEcontinuarEmitindoMesmosTokens() throws Exception {
        String email = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, email);

        mockMvc.perform(post("/api/v1/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Sunset", "Wed, 31 Mar 2027 00:00:00 GMT"))
                .andExpect(header().string(HttpHeaders.LINK,
                        "</api/v1/auth/login>; rel=\"successor-version\""))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"%s"}
                                """.formatted(email, TestAutenticacao.SENHA)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"));
    }

    @Test
    void rotaProtegidaDeveExigirAccessTokenValido() throws Exception {
        String jsonAnuncio = """
                {"titulo":"Livro","preco":10.00,"categoria":"LIVROS"}
                """;

        mockMvc.perform(post("/api/v1/anuncios")
                        .contentType(MediaType.APPLICATION_JSON).content(jsonAnuncio))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));

        mockMvc.perform(post("/api/v1/anuncios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido")
                        .contentType(MediaType.APPLICATION_JSON).content(jsonAnuncio))
                .andExpect(status().isUnauthorized());

        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        mockMvc.perform(post("/api/v1/anuncios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonAnuncio))
                .andExpect(status().isOk());
    }

    @Test
    void refreshDeveRotacionarERevogarTokenAnterior() throws Exception {
        String email = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, email);
        String refreshAntigo = TestAutenticacao.refreshToken(mockMvc, email);

        String corpo = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshAntigo)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshNovo = JsonPath.read(corpo, "$.refreshToken");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshAntigo)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshNovo)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotacionadoDeveSerAceito() throws Exception {
        String email = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, email);
        String refreshAntigo = TestAutenticacao.refreshToken(mockMvc, email);

        String corpo = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshAntigo)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshNovo = JsonPath.read(corpo, "$.refreshToken");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshNovo)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutDeveRevogarRefreshToken() throws Exception {
        String email = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, email);
        String access = TestAutenticacao.login(mockMvc, email);
        String refresh = TestAutenticacao.refreshToken(mockMvc, email);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutDeveRevogarAccessTokenImediatamente() throws Exception {
        String email = TestAutenticacao.novoEmail();
        TestAutenticacao.cadastrar(mockMvc, email);
        String access = TestAutenticacao.login(mockMvc, email);

        mockMvc.perform(get("/api/v1/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/usuarios/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenNaoDeveServirComoRefreshToken() throws Exception {
        String access = TestAutenticacao.cadastrarELogar(mockMvc);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(access)))
                .andExpect(status().isUnauthorized());
    }
}
