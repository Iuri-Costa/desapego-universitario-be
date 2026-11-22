package com.marketplace.desapegoUniversitario.dto;

import com.marketplace.desapegoUniversitario.domain.MotivoDenuncia;

import java.time.Instant;
import java.util.UUID;

public record DenunciaResponseDTO(
        UUID id,
        UUID anuncioId,
        UUID denuncianteId,
        MotivoDenuncia motivo,
        String descricao,
        String status,
        Instant resolvidaEm,
        Instant criadoEm
) {
}
