package com.marketplace.desapegoUniversitario.dto;

import jakarta.validation.constraints.NotNull;

public record ModeracaoDTO(
        @NotNull(message = "O campo moderado é obrigatório")
        Boolean moderado
) {
}
