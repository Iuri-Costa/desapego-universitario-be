package com.marketplace.desapegoUniversitario.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

public record ErroResponse(
        @Schema(example = "2026-11-22T16:00:00Z")
        OffsetDateTime timestamp,

        @Schema(example = "400")
        int status,

        @Schema(description = "Código estável para o cliente tratar o erro", example = "VALIDACAO")
        String codigo,

        @Schema(example = "Erro de validação")
        String mensagem,

        @Schema(example = "/api/v1/anuncios")
        String caminho,

        List<ErroCampo> erros
) {
}
