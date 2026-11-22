package com.marketplace.desapegoUniversitario.security;

import com.marketplace.desapegoUniversitario.exception.ErroResponse;
import com.marketplace.desapegoUniversitario.exception.ErroResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final ErroResponseFactory erroFactory;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper, ErroResponseFactory erroFactory) {
        this.objectMapper = objectMapper;
        this.erroFactory = erroFactory;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErroResponse erro = erroFactory.criar(status, "NAO_AUTENTICADO",
                "Autenticação necessária para acessar este recurso", request, List.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), erro);
    }
}
