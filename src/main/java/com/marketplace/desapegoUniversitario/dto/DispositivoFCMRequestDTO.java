package com.marketplace.desapegoUniversitario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DispositivoFCMRequestDTO(
        @NotBlank(message = "O token do dispositivo é obrigatório")
        @Size(max = 512, message = "O token do dispositivo deve ter no máximo 512 caracteres")
        String token
) {
}
