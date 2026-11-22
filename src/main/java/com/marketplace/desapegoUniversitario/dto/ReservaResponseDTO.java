package com.marketplace.desapegoUniversitario.dto;

import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.StatusReserva;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record ReservaResponseDTO(
        @Schema(description = "Identificador da reserva") UUID id,
        @Schema(description = "Anúncio reservado") UUID anuncioId,
        @Schema(description = "Título do anúncio") String anuncioTitulo,
        @Schema(description = "Status atual do anúncio") StatusAnuncio anuncioStatus,
        @Schema(description = "Comprador da reserva") UUID compradorId,
        @Schema(description = "Nome do comprador") String compradorNome,
        @Schema(description = "Vendedor do anúncio") UUID vendedorId,
        @Schema(description = "Situação da reserva") StatusReserva status,
        @Schema(description = "Quando a reserva foi criada") Instant criadoEm,
        @Schema(description = "Quando a reserva foi cancelada") Instant canceladaEm,
        @Schema(description = "Quando a reserva foi concluída") Instant concluidaEm,
        @Schema(description = "Quando a reserva expirou") Instant expiradaEm
) {
}
