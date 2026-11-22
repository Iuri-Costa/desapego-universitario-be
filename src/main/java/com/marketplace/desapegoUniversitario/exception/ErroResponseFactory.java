package com.marketplace.desapegoUniversitario.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class ErroResponseFactory {

    public ErroResponse criar(HttpStatus status, String codigo, String mensagem,
                              HttpServletRequest request, List<ErroCampo> erros) {
        return new ErroResponse(
                OffsetDateTime.now(),
                status.value(),
                codigo,
                mensagem,
                request.getRequestURI(),
                erros == null ? List.of() : erros);
    }
}
