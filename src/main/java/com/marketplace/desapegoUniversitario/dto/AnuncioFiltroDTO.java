package com.marketplace.desapegoUniversitario.dto;

import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.domain.Condicao;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.TipoAnuncio;

import java.math.BigDecimal;

public record AnuncioFiltroDTO(
        String texto,
        Categoria categoria,
        TipoAnuncio tipo,
        BigDecimal precoMin,
        BigDecimal precoMax,
        Condicao condicao,
        String campus,
        StatusAnuncio status,
        int page,
        int size,
        String sort
) {
}
