package com.marketplace.desapegoUniversitario.dto;

import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record ConversaResponseDTO(
        @Schema(description = "Anúncio da conversa") UUID anuncioId,
        @Schema(description = "Título do anúncio") String anuncioTitulo,
        @Schema(description = "Status atual do anúncio") StatusAnuncio anuncioStatus,
        @Schema(description = "Comprador envolvido na reserva") UUID compradorId,
        @Schema(description = "Vendedor do anúncio") UUID vendedorId,
        @Schema(description = "Outro participante da conversa") UUID outroUsuarioId,
        @Schema(description = "Nome do outro participante") String outroUsuarioNome,
        @Schema(description = "Conteúdo da última mensagem") String ultimaMensagem,
        @Schema(description = "Data da última mensagem") Instant ultimaMensagemEm,
        @Schema(description = "Mensagens recebidas ainda não lidas") long mensagensNaoLidas
) {
}
