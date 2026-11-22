package com.marketplace.desapegoUniversitario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DocumentacaoApiIntegrationTest {

    private static final Path API_MD = Path.of("docs/API.md");
    private static final String PACOTE_CONTROLLERS =
            "com.marketplace.desapegoUniversitario.controller";

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void documentacaoDeveListarTodosOsEndpointsDosControllers() throws IOException {
        String documentacao = Files.readString(API_MD);

        for (var entrada : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = entrada.getValue();
            if (!handler.getBeanType().getPackageName().equals(PACOTE_CONTROLLERS)) {
                continue;
            }
            for (String padrao : entrada.getKey().getPatternValues()) {
                String sufixo = padrao.startsWith("/api/v1") ? padrao.substring("/api/v1".length()) : padrao;
                assertTrue(documentacao.contains(sufixo),
                        "docs/API.md não documenta o endpoint " + padrao);
            }
        }
    }

    @Test
    void documentacaoDeveRefletirCorrecoesRecentes() throws IOException {
        String documentacao = Files.readString(API_MD);

        assertTrue(documentacao.contains("/v3/api-docs"));
        assertTrue(documentacao.contains("correlationId"));
        assertTrue(documentacao.contains("LIMITE_EXCEDIDO"));
        assertTrue(documentacao.contains("ADMIN_BOOTSTRAP_EMAIL"));
        assertTrue(documentacao.contains("@PreAuthorize"));
    }
}
