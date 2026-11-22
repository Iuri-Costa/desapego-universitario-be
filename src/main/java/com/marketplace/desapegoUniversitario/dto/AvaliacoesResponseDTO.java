package com.marketplace.desapegoUniversitario.dto;

import java.util.List;

public record AvaliacoesResponseDTO(double media, long total, List<AvaliacaoResponseDTO> avaliacoes) {
}
