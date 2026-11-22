package com.marketplace.desapegoUniversitario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.desapegoUniversitario.service.FcmNotificadorPush;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FcmNotificadorPushTest {

    private static final String OAUTH_URL = "http://localhost/oauth";
    private static final String SEND_URL = "http://localhost/fcm";
    private static final String SEND_URI = SEND_URL + "/projects/test-project/messages:send";
    private static final String TOKEN_RESPONSE = "{\"access_token\":\"oauth-token\",\"expires_in\":3600}";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer server;
    private FcmNotificadorPush notificador;
    private String pem;

    @BeforeEach
    void setUp() throws Exception {
        KeyPair par = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(par.getPrivate().getEncoded());
        pem = "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
        notificador = novoNotificador(credenciais(
                "firebase@test-project.iam.gserviceaccount.com", "test-project", pem));
    }

    private String credenciais(String email, String projeto, String chave) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "private_key", chave,
                "client_email", email,
                "project_id", projeto));
    }

    private FcmNotificadorPush novoNotificador(String json) throws Exception {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new FcmNotificadorPush(builder.build(), objectMapper, json,
                "", "escopo-teste", OAUTH_URL, SEND_URL);
    }

    private void esperarOAuth() {
        server.expect(requestTo(OAUTH_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("grant_type=")))
                .andExpect(content().string(containsString("assertion=")))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
    }

    private void esperarEnvio() {
        server.expect(requestTo(SEND_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer oauth-token"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("{\"name\":\"projects/test-project/messages/1\"}",
                        MediaType.APPLICATION_JSON));
    }

    @Test
    void deveEnviarPushPeloCaminhoFCM() {
        esperarOAuth();
        esperarEnvio();

        assertTrue(notificador.enviar("device-token", "Titulo", "Mensagem"));
        server.verify();
    }

    @Test
    void deveReutilizarTokenOAuthCacheado() {
        esperarOAuth();
        esperarEnvio();
        esperarEnvio();

        assertTrue(notificador.enviar("token-1", "Titulo", "Mensagem 1"));
        assertTrue(notificador.enviar("token-2", "Titulo", "Mensagem 2"));
        server.verify();
    }

    @Test
    void deveInvalidarTokenNaoRegistrado() {
        esperarOAuth();
        server.expect(requestTo(SEND_URI)).andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":404,\"status\":\"UNREGISTERED\"}}"));

        assertFalse(notificador.enviar("token-antigo", "Titulo", "Mensagem"));
        server.verify();
    }

    @Test
    void deveInvalidarTokenComArgumentoInvalido() {
        esperarOAuth();
        server.expect(requestTo(SEND_URI)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":400,\"status\":\"INVALID_ARGUMENT\","
                        + "\"details\":[{\"@type\":\"type.googleapis.com/google.firebase.fcm.v1.FcmError\","
                        + "\"errorCode\":\"INVALID_ARGUMENT\"}]}}"));

        assertFalse(notificador.enviar("token-invalido", "Titulo", "Mensagem"));
        server.verify();
    }

    @Test
    void deveManterTokenEmFalhaTransitoria() {
        esperarOAuth();
        server.expect(requestTo(SEND_URI)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":503}}"));

        assertTrue(notificador.enviar("token-valido", "Titulo", "Mensagem"));
        server.verify();
    }

    @Test
    void tokenEmBrancoNaoChamaFCM() {
        assertTrue(notificador.enviar("  ", "Titulo", "Mensagem"));
        assertTrue(notificador.enviar(null, "Titulo", "Mensagem"));
        server.verify();
    }

    @Test
    void deveRejeitarCredenciaisIncompletas() throws Exception {
        String json = credenciais("", "", pem);
        RestClient.Builder builder = RestClient.builder();

        assertThrows(IllegalArgumentException.class, () -> new FcmNotificadorPush(
                builder.build(), objectMapper, json, "", "escopo-teste", OAUTH_URL, SEND_URL));
    }
}
