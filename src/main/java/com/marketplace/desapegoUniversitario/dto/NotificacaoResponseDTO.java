package com.marketplace.desapegoUniversitario.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificacaoResponseDTO(
        UUID id,
        String titulo,
        String mensagem,
        String tipo,
        UUID referenciaId,
        Instant criadoEm,
        Instant lidaEm
) {
}
