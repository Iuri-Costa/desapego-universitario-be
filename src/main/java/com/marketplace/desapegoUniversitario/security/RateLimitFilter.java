package com.marketplace.desapegoUniversitario.security;

import com.marketplace.desapegoUniversitario.exception.ErroResponseFactory;
import com.marketplace.desapegoUniversitario.exception.LimiteExcedidoException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, LimitadorRequisicoes.Tipo> ROTAS = Map.of(
            "/api/v1/usuarios/login", LimitadorRequisicoes.Tipo.LOGIN,
            "/api/v1/auth/login", LimitadorRequisicoes.Tipo.LOGIN,
            "/api/v1/usuarios/cadastro", LimitadorRequisicoes.Tipo.CADASTRO);

    private final LimitadorRequisicoes limitador;
    private final ErroResponseFactory erroFactory;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(LimitadorRequisicoes limitador, ErroResponseFactory erroFactory,
                           ObjectMapper objectMapper) {
        this.limitador = limitador;
        this.erroFactory = erroFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !ROTAS.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LimitadorRequisicoes.Tipo tipo = ROTAS.get(request.getRequestURI());
        try {
            limitador.verificarIp(tipo, request.getRemoteAddr());
        } catch (LimiteExcedidoException ex) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSegundos()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), erroFactory.criar(
                    HttpStatus.TOO_MANY_REQUESTS, "LIMITE_EXCEDIDO", ex.getMessage(), request, null));
            return;
        }
        chain.doFilter(request, response);
    }

    public void limpar() {
        limitador.limpar();
    }
}
