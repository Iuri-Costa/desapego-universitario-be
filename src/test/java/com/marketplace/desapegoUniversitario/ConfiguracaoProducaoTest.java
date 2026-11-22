package com.marketplace.desapegoUniversitario;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracaoProducaoTest {

    private static final Path PROD = Path.of("src/main/resources/application-prod.properties");
    private static final Path COMPOSE = Path.of("docker-compose.yml");
    private static final Path ENV_EXEMPLO = Path.of(".env.example");

    @Test
    void segredosNaoDevemTerDefaultNoPerfilProd() throws IOException {
        String conteudo = Files.readString(PROD);

        for (String secret : List.of("app.jwt.secret=${JWT_SECRET}", "app.mail.username=${MAIL_USERNAME}",
                "app.mail.password=${MAIL_PASSWORD}", "app.storage.s3.access-key=${S3_ACCESS_KEY}",
                "app.storage.s3.secret-key=${S3_SECRET_KEY}", "app.fcm.credentials=${FCM_CREDENTIALS}",
                "app.fcm.project-id=${FCM_PROJECT_ID}")) {
            assertTrue(conteudo.contains(secret), "deve exigir: " + secret);
        }
        assertFalse(conteudo.contains("${JWT_SECRET:"));
        assertFalse(conteudo.contains("${MAIL_PASSWORD:"));
        assertFalse(conteudo.contains("${S3_SECRET_KEY:"));
    }

    @Test
    void composeNaoDeveTrazerSenhasOuSegredosPadrao() throws IOException {
        String conteudo = Files.readString(COMPOSE);

        assertTrue(conteudo.contains("${DB_PASSWORD:?defina DB_PASSWORD no .env}"));
        assertTrue(conteudo.contains("${JWT_SECRET:?defina JWT_SECRET no .env"));
        assertFalse(conteudo.contains("${DB_PASSWORD:-postgres}"));
        assertFalse(conteudo.contains("${JWT_SECRET:-troque"));
        assertFalse(conteudo.contains("MAIL_PASSWORD:-"));
        assertFalse(conteudo.contains("S3_SECRET_KEY:-"));
        assertFalse(conteudo.contains("FCM_CREDENTIALS:-"));
    }

    @Test
    void envExampleDeveEstarCompletoSemSegredoValido() throws IOException {
        String conteudo = Files.readString(ENV_EXEMPLO);

        for (String variavel : List.of("API_PORT", "DB_NAME", "DB_PORT", "DB_PASSWORD", "JWT_SECRET",
                "ADMIN_BOOTSTRAP_EMAIL", "MAIL_USERNAME", "MAIL_PASSWORD", "S3_ACCESS_KEY", "S3_SECRET_KEY",
                "FCM_CREDENTIALS", "FCM_PROJECT_ID", "OPENAPI_ENABLED", "SWAGGER_UI_ENABLED", "APP_VERSION")) {
            assertTrue(conteudo.contains(variavel + "="), "variável ausente: " + variavel);
        }
        assertFalse(conteudo.contains("JWT_SECRET=troque"));
        assertFalse(conteudo.contains("DB_PASSWORD=postgres"));
    }
}
