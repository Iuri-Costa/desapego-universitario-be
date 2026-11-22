package com.marketplace.desapegoUniversitario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponseDTO(
        @Schema(description = "Token de acesso JWT (Bearer), válido por 15 minutos",
                example = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhbHVub0B1bml2ZXJzaWRhZGUuZWR1LmJyIn0.exemplo")
        String accessToken,

        @Schema(description = "Token de renovação, válido por 7 dias e de uso único",
                example = "eyJhbGciOiJIUzM4NCJ9.eyJ0aXBvIjoicmVmcmVzaCJ9.exemplo")
        String refreshToken,

        @Schema(description = "Tipo do token", example = "Bearer")
        String tipo,

        @Schema(description = "Validade do access token em segundos", example = "900")
        long expiresIn
) {
}
