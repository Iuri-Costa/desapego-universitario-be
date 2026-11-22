package com.marketplace.desapegoUniversitario.dto;

import java.util.UUID;

public record ImagemResponseDTO(UUID id, String url, Integer ordem, boolean capa) {
}
