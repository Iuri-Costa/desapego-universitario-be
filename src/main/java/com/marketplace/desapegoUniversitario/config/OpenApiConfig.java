package com.marketplace.desapegoUniversitario.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    public static final String SEGURANCA_BEARER = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Access token JWT. Envie como `Authorization: Bearer <accessToken>`.");

        return new OpenAPI()
                .info(new Info()
                        .title("Desapego Universitário API")
                        .version("v1")
                        .description("""
                                API REST do marketplace universitário. Todos os identificadores são UUID,
                                datas em ISO 8601 UTC, paginação com `page`/`size`/`sort` e erros no formato
                                `ErroResponse`. Use `POST /api/v1/auth/login` para obter o access token e
                                `POST /api/v1/auth/refresh` para renová-lo. Política de versionamento e
                                depreciação: `docs/VERSIONAMENTO.md`.
                                """))
                .servers(List.of(new Server().url("/").description("Servidor atual")))
                .components(new Components().addSecuritySchemes(SEGURANCA_BEARER, bearer))
                .addSecurityItem(new SecurityRequirement().addList(SEGURANCA_BEARER));
    }

    @Bean
    public OperationCustomizer respostasDeErro() {
        Map<String, String> erros = Map.of(
                "400", "Requisição inválida (validação)",
                "401", "Não autenticado ou token inválido",
                "403", "Sem permissão para a operação",
                "404", "Recurso não encontrado",
                "409", "Conflito de estado",
                "429", "Limite de requisições excedido");

        return (operation, handlerMethod) -> {
            if (operation.getResponses() == null) {
                operation.setResponses(new ApiResponses());
            }
            erros.forEach((codigo, descricao) -> operation.getResponses().putIfAbsent(codigo,
                    new ApiResponse().description(descricao)
                            .content(new Content().addMediaType("application/json",
                                    new MediaType().schema(new io.swagger.v3.oas.models.media.Schema()
                                            .$ref("#/components/schemas/ErroResponse"))))));
            return operation;
        };
    }
}
