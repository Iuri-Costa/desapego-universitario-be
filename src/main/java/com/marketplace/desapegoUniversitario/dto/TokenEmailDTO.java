package com.marketplace.desapegoUniversitario.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenEmailDTO(
        @NotBlank(message = "O token é obrigatório")
        String token
) {
}
