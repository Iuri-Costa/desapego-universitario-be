package com.marketplace.desapegoUniversitario.dto;

import java.time.Instant;
import java.util.UUID;

public record AvaliacaoResponseDTO(
        UUID id,
        UUID anuncioId,
        UUID vendedorId,
        UUID avaliadorId,
        int nota,
        String comentario,
        Instant criadoEm
) {
}
