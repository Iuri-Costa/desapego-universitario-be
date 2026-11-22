package com.marketplace.desapegoUniversitario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MensagemRequestDTO(
        UUID destinatarioId,
        @NotBlank(message = "A mensagem não pode ser vazia")
        @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres")
        String conteudo
) {
}
