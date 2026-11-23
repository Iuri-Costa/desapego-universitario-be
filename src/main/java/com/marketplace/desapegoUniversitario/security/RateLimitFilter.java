package com.marketplace.desapegoUniversitario.security;

import com.marketplace.desapegoUniversitario.exception.ErroResponseFactory;
import com.marketplace.desapegoUniversitario.exception.LimiteExcedidoException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, LimitadorRequisicoes.Tipo> ROTAS = Map.of(
            "/api/v1/usuarios/login", LimitadorRequisicoes.Tipo.LOGIN,
            "/api/v1/auth/login", LimitadorRequisicoes.Tipo.LOGIN,
            "/api/v1/usuarios/cadastro", LimitadorRequisicoes.Tipo.CADASTRO);

    private static final Pattern IPV4 = Pattern.compile(
            "(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)"
                    + "(?:\\.(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}");
    private static final Pattern IPV6 = Pattern.compile("[0-9a-fA-F:]{2,45}");

    private final LimitadorRequisicoes limitador;
    private final ErroResponseFactory erroFactory;
    private final ObjectMapper objectMapper;
    private final Pattern trustedProxies;

    public RateLimitFilter(LimitadorRequisicoes limitador, ErroResponseFactory erroFactory,
                           ObjectMapper objectMapper,
                           @Value("${app.rate-limit.trusted-proxies:}") String trustedProxies) {
        this.limitador = limitador;
        this.erroFactory = erroFactory;
        this.objectMapper = objectMapper;
        this.trustedProxies = trustedProxies == null || trustedProxies.isBlank()
                ? null : Pattern.compile(trustedProxies);
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
            limitador.verificarIp(tipo, ipCliente(request));
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

    String ipCliente(HttpServletRequest request) {
        String remoteAddr = normalizarIp(request.getRemoteAddr());
        if (remoteAddr == null) {
            return "desconhecido";
        }
        if (trustedProxies != null && trustedProxies.matcher(remoteAddr).matches()) {
            String encaminhado = primeiroIpEncaminhado(request.getHeader("X-Forwarded-For"));
            if (encaminhado != null) {
                return encaminhado;
            }
        }
        return remoteAddr;
    }

    private String primeiroIpEncaminhado(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String primeiro = header.split(",", 2)[0].trim();
        return normalizarIp(primeiro);
    }

    private String normalizarIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        String candidato = ip.trim();
        if (candidato.startsWith("[") && candidato.contains("]")) {
            candidato = candidato.substring(1, candidato.indexOf(']'));
        } else if (IPV4.matcher(candidato).matches()) {
            return candidato;
        } else if (candidato.indexOf(':') >= 0 && candidato.indexOf(':') == candidato.lastIndexOf(':')
                && candidato.contains(".")) {
            candidato = candidato.substring(0, candidato.lastIndexOf(':'));
        }
        if (IPV4.matcher(candidato).matches() || IPV6.matcher(candidato).matches()) {
            return candidato;
        }
        return null;
    }

    public void limpar() {
        limitador.limpar();
    }
}
