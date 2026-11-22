package com.marketplace.desapegoUniversitario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Schema(example = "aluno@universidade.edu.br")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Schema(example = "Senha@12345")
        String senha
) {
}
