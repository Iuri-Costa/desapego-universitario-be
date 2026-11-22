package com.marketplace.desapegoUniversitario.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Optional;

public record AtualizarPerfilDTO(
        Optional<@Pattern(regexp = ".*\\S.*", message = "O nome é obrigatório")
                @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres") String> nome,
        Optional<@Size(max = 255, message = "O contato deve ter no máximo 255 caracteres") String> contato,
        Optional<@Size(max = 1000, message = "O avatar deve ter no máximo 1000 caracteres")
                @Pattern(regexp = "^(|/uploads/\\S+|https://\\S+)$",
                        message = "Use /uploads/... ou uma URL https") String> avatar
) {
}
