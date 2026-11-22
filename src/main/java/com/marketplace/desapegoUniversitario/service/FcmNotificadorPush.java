package com.marketplace.desapegoUniversitario.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class FcmNotificadorPush implements NotificadorPush {

    private static final Logger log = LoggerFactory.getLogger(FcmNotificadorPush.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PrivateKey privateKey;
    private final String clientEmail;
    private final String projectId;
    private final String scope;
    private final String oauthUrl;
    private final String sendUrl;

    private volatile String accessToken;
    private volatile Instant expiracao;

    public FcmNotificadorPush(RestClient restClient, ObjectMapper objectMapper, String jsonCredenciais,
                              String projectId, String scope, String oauthUrl, String sendUrl) throws Exception {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        JsonNode credenciais = objectMapper.readTree(jsonCredenciais);
        this.privateKey = lerChavePrivada(credenciais.path("private_key").asText());
        this.clientEmail = credenciais.path("client_email").asText();
        String projeto = projectId.isBlank()
                ? credenciais.path("project_id").asText() : projectId;
        if (clientEmail.isBlank() || projeto.isBlank()) {
            throw new IllegalArgumentException("Credenciais FCM incompletas");
        }
        this.projectId = projeto;
        this.scope = scope;
        this.oauthUrl = oauthUrl;
        this.sendUrl = sendUrl;
    }

    @Override
    public boolean enviar(String tokenDispositivo, String titulo, String mensagem) {
        if (tokenDispositivo == null || tokenDispositivo.isBlank()) {
            return true;
        }
        try {
            Map<String, Object> corpo = Map.of("message", Map.of(
                    "token", tokenDispositivo,
                    "notification", Map.of("title", titulo, "body", mensagem)));
            restClient.post()
                    .uri(sendUrl + "/projects/" + projectId + "/messages:send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + tokenAcesso())
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException e) {
            if (tokenInvalidado(e.getStatusCode(), e.getResponseBodyAsString())) {
                log.warn("Token FCM inválido; status={}", e.getStatusCode().value());
                return false;
            }
            log.warn("Falha ao enviar notificação push FCM: status={}", e.getStatusCode().value());
            return true;
        } catch (RuntimeException e) {
            log.warn("Falha ao enviar notificação push FCM: {}", e.getClass().getSimpleName());
            return true;
        }
    }

    private boolean tokenInvalidado(HttpStatusCode status, String corpo) {
        int codigo = status.value();
        if (codigo == 404 || codigo == 403) {
            return true;
        }
        if (codigo != 400 || corpo == null) {
            return false;
        }
        String resposta = corpo.toUpperCase(Locale.ROOT);
        return resposta.contains("FCMERROR")
                || resposta.contains("INVALID_ARGUMENT")
                || resposta.contains("REGISTRATION TOKEN");
    }

    private String tokenAcesso() {
        if (accessToken != null && expiracao != null && Instant.now().isBefore(expiracao)) {
            return accessToken;
        }
        synchronized (this) {
            if (accessToken != null && expiracao != null && Instant.now().isBefore(expiracao)) {
                return accessToken;
            }
            String assertion = Jwts.builder()
                    .issuer(clientEmail)
                    .subject(clientEmail)
                    .audience().add(oauthUrl).and()
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(3600)))
                    .claim("scope", scope)
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();

            String corpo = restClient.post()
                    .uri(oauthUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" + assertion)
                    .retrieve()
                    .body(String.class);
            JsonNode resposta;
            try {
                resposta = objectMapper.readTree(corpo == null ? "{}" : corpo);
            } catch (Exception e) {
                throw new IllegalStateException("Resposta OAuth inválida", e);
            }
            accessToken = resposta.path("access_token").asText();
            long segundos = resposta.path("expires_in").asLong(3600);
            expiracao = Instant.now().plusSeconds(Math.max(60, segundos - 60));
            return accessToken;
        }
    }

    private PrivateKey lerChavePrivada(String pem) throws Exception {
        String limpo = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(limpo);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }
}
