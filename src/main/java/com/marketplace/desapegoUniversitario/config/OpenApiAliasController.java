package com.marketplace.desapegoUniversitario.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiAliasController {

    private final OpenApiWebMvcResource openApiResource;

    public OpenApiAliasController(OpenApiWebMvcResource openApiResource) {
        this.openApiResource = openApiResource;
    }

    @Operation(hidden = true)
    @GetMapping(value = "/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public byte[] openapi(HttpServletRequest request, Locale locale) throws JsonProcessingException {
        return openApiResource.openapiJson(request, "/v3/api-docs", locale);
    }
}
