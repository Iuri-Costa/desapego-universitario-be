package com.marketplace.desapegoUniversitario.dto;

import com.marketplace.desapegoUniversitario.domain.Categoria;

import java.math.BigDecimal;
import java.util.UUID;

public record AnuncioResponseDTO(
        UUID id,
        String titulo,
        BigDecimal preco,
        String descricao,
        String imagem,
        Categoria categoria,
        String contato,
        boolean moderado
) {
}
