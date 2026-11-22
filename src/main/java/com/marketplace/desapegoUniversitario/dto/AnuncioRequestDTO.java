package com.marketplace.desapegoUniversitario.dto;

import com.marketplace.desapegoUniversitario.domain.Categoria;

import java.math.BigDecimal;

public record AnuncioRequestDTO(
        String titulo,
        BigDecimal preco,
        String descricao,
        String imagem,
        Categoria categoria,
        String contato
) {
}
