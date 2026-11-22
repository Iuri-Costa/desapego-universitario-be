package com.marketplace.desapegoUniversitario.dto;

import com.marketplace.desapegoUniversitario.domain.Categoria;
import com.marketplace.desapegoUniversitario.domain.Condicao;
import com.marketplace.desapegoUniversitario.domain.TipoAnuncio;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AnuncioRequestDTO(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        String titulo,

        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.0", inclusive = true, message = "O preço não pode ser negativo")
        @Digits(integer = 10, fraction = 2, message = "O preço deve ter até 10 dígitos inteiros e 2 casas decimais")
        BigDecimal preco,

        @Size(max = 5000, message = "A descrição deve ter no máximo 5000 caracteres")
        String descricao,

        String imagem,

        @NotNull(message = "A categoria é obrigatória")
        Categoria categoria,

        @Size(max = 255, message = "O contato deve ter no máximo 255 caracteres")
        String contato,

        TipoAnuncio tipo,

        Condicao condicao,

        @Size(max = 120, message = "O campus deve ter no máximo 120 caracteres")
        String campus,

        @Size(max = 255, message = "O local de retirada deve ter no máximo 255 caracteres")
        String localRetirada
) {
}
