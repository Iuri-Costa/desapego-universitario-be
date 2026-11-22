package com.marketplace.desapegoUniversitario.dto;

import java.time.Instant;
import java.util.UUID;

public record DenunciaResponseDTO(
        UUID id,
        UUID anuncioId,
        UUID denuncianteId,
        String motivo,
        String descricao,
        String status,
        Instant resolvidaEm,
        Instant criadoEm
) {
}
