package com.marketplace.desapegoUniversitario.security;

import com.marketplace.desapegoUniversitario.exception.ErroResponse;
import com.marketplace.desapegoUniversitario.exception.ErroResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ErroResponseFactory erroFactory;

    public RestAccessDeniedHandler(ObjectMapper objectMapper, ErroResponseFactory erroFactory) {
        this.objectMapper = objectMapper;
        this.erroFactory = erroFactory;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        HttpStatus status = HttpStatus.FORBIDDEN;
        ErroResponse erro = erroFactory.criar(status, "ACESSO_NEGADO",
                "Você não tem permissão para acessar este recurso", request, List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), erro);
    }
}
