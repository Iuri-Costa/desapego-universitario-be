package com.marketplace.desapegoUniversitario.dto;

public record TokenResponseDTO(
        String accessToken,
        String refreshToken,
        String tipo,
        long expiresIn
) {
}
