package com.marketplace.desapegoUniversitario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarPerfilDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String nome
) {
}
