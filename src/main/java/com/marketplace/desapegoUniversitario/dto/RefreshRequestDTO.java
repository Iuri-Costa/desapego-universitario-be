package com.marketplace.desapegoUniversitario.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDTO(
        @NotBlank(message = "O refresh token é obrigatório")
        String refreshToken
) {
}
