package com.marketplace.desapegoUniversitario.dto;

import com.marketplace.desapegoUniversitario.validation.SenhaForte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaDTO(
        @NotBlank(message = "O token é obrigatório")
        String token,

        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        @SenhaForte
        String novaSenha
) {
}
