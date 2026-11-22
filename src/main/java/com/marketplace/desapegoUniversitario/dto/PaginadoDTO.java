package com.marketplace.desapegoUniversitario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PaginadoDTO<T>(
        @Schema(description = "Itens da página atual") List<T> content,
        @Schema(description = "Índice da página (base 0)", example = "0") int page,
        @Schema(description = "Tamanho da página", example = "20") int size,
        @Schema(description = "Total de itens em todas as páginas", example = "137") long totalElements,
        @Schema(description = "Total de páginas", example = "7") int totalPages,
        @Schema(description = "Primeira página", example = "true") boolean first,
        @Schema(description = "Última página", example = "false") boolean last
) {
    public static <E, T> PaginadoDTO<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new PaginadoDTO<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast()
        );
    }
}
