package com.marketplace.desapegoUniversitario.dto;

import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.domain.Condicao;
import com.marketplace.desapegoUniversitario.domain.TipoAnuncio;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Optional;

public record AnuncioPatchDTO(
        Optional<@NotBlank @Size(max = 150) String> titulo,
        Optional<@DecimalMin(value = "0.0", inclusive = true) @Digits(integer = 10, fraction = 2) BigDecimal> preco,
        Optional<@Size(max = 5000) String> descricao,
        Optional<String> imagem,
        Optional<Categoria> categoria,
        Optional<@Size(max = 255) String> contato,
        Optional<TipoAnuncio> tipo,
        Optional<Condicao> condicao,
        Optional<@Size(max = 120) String> campus,
        Optional<@Size(max = 255) String> localRetirada
) {
}
