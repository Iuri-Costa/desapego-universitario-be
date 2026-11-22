package com.marketplace.desapegoUniversitario.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ErroResponse(
        OffsetDateTime timestamp,
        int status,
        String codigo,
        String mensagem,
        String caminho,
        List<ErroCampo> erros
) {
}
