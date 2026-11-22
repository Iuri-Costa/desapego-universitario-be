package com.marketplace.desapegoUniversitario.dto;

import com.marketplace.desapegoUniversitario.domain.MotivoDenuncia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DenunciaRequestDTO(
        @NotNull(message = "O motivo é obrigatório")
        MotivoDenuncia motivo,
        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String descricao
) {
}
