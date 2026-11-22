package com.marketplace.desapegoUniversitario.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.desapegoUniversitario.service.FcmNotificadorPush;
import com.marketplace.desapegoUniversitario.service.LocalNotificadorPush;
import com.marketplace.desapegoUniversitario.service.NotificadorPush;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class FcmConfig {

    @Bean
    @Conditional(FcmCondition.class)
    public RestClient fcmRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }

    @Bean
    @Conditional(FcmCondition.class)
    public FcmNotificadorPush fcmNotificadorPush(
            RestClient fcmRestClient,
            ObjectMapper objectMapper,
            @Value("${app.fcm.credentials}") String credenciais,
            @Value("${app.fcm.project-id:}") String projectId,
            @Value("${app.fcm.scope:https://www.googleapis.com/auth/firebase.messaging}") String scope,
            @Value("${app.fcm.oauth-url:https://oauth2.googleapis.com/token}") String oauthUrl,
            @Value("${app.fcm.send-url:https://fcm.googleapis.com/v1}") String sendUrl) throws Exception {
        String json = credenciais.trim().startsWith("{")
                ? credenciais
                : Files.readString(Path.of(credenciais));
        return new FcmNotificadorPush(fcmRestClient, objectMapper, json, projectId, scope, oauthUrl, sendUrl);
    }

    @Bean
    @Conditional(FcmAusenteCondition.class)
    public NotificadorPush notificadorPushLocal() {
        return new LocalNotificadorPush();
    }
}
