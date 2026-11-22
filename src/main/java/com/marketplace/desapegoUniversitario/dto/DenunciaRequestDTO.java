package com.marketplace.desapegoUniversitario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DenunciaRequestDTO(
        @NotBlank(message = "O motivo é obrigatório")
        @Size(max = 50, message = "O motivo deve ter no máximo 50 caracteres")
        String motivo,
        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String descricao
) {
}
