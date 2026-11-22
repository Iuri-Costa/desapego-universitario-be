package com.marketplace.desapegoUniversitario.dto;

import java.time.Instant;
import java.util.UUID;

public record MensagemResponseDTO(
        UUID id,
        UUID anuncioId,
        UUID remetenteId,
        String remetenteNome,
        UUID destinatarioId,
        String conteudo,
        Instant criadoEm,
        Instant lidaEm
) {
}
