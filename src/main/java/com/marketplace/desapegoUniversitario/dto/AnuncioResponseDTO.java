package com.marketplace.desapegoUniversitario.dto;

import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.domain.Condicao;
import com.marketplace.desapegoUniversitario.domain.StatusAnuncio;
import com.marketplace.desapegoUniversitario.domain.TipoAnuncio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnuncioResponseDTO(
        UUID id,
        String titulo,
        BigDecimal preco,
        String descricao,
        String imagem,
        Categoria categoria,
        String contato,
        boolean moderado,
        TipoAnuncio tipo,
        Condicao condicao,
        StatusAnuncio status,
        String campus,
        String localRetirada,
        Instant criadoEm,
        Instant atualizadoEm,
        List<ImagemResponseDTO> imagens
) {
}
