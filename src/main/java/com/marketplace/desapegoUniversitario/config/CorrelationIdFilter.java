package com.marketplace.desapegoUniversitario.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    private static final String ATRIBUTO = CorrelationIdFilter.class.getName() + ".ID";
    private static final int TAMANHO_MAXIMO = 64;
    private static final String VALIDO = "[A-Za-z0-9._-]+";

    public static String obter(HttpServletRequest request) {
        Object valor = request.getAttribute(ATRIBUTO);
        if (valor instanceof String texto && !texto.isBlank()) {
            return texto;
        }
        String cabecalho = request.getHeader(HEADER);
        if (cabecalho != null && !cabecalho.isBlank()
                && cabecalho.length() <= TAMANHO_MAXIMO && cabecalho.matches(VALIDO)) {
            return cabecalho;
        }
        String gerado = UUID.randomUUID().toString();
        request.setAttribute(ATRIBUTO, gerado);
        return gerado;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = obter(request);
        request.setAttribute(ATRIBUTO, correlationId);
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
