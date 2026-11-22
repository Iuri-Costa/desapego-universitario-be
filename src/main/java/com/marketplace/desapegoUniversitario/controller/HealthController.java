package com.marketplace.desapegoUniversitario.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Health", description = "Health check leve para inicialização e orquestradores")
public class HealthController {

    @GetMapping("/healthz")
    @Operation(summary = "Health check leve", description = "Responde 200 sem consultar dependências externas")
    public Map<String, String> healthz() {
        return Map.of("status", "ok");
    }
}
