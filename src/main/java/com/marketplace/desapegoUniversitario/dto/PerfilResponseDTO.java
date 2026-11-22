package com.marketplace.desapegoUniversitario.dto;

import java.time.Instant;
import java.util.UUID;

public record PerfilResponseDTO(
        UUID id,
        String nome,
        String email,
        String role,
        boolean emailVerificado,
        Instant criadoEm,
        long totalAnuncios,
        long anunciosAtivos,
        long favoritos
) {
}
