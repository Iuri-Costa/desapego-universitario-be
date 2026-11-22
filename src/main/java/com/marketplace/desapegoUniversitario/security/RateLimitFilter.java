package com.marketplace.desapegoUniversitario.security;

import tools.jackson.databind.ObjectMapper;
import com.marketplace.desapegoUniversitario.exception.ErroResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN = "/api/v1/usuarios/login";
    private static final String CADASTRO = "/api/v1/usuarios/cadastro";
    private static final List<String> ROTAS = List.of(LOGIN, CADASTRO);

    private final Map<String, Deque<Instant>> janelas = new ConcurrentHashMap<>();
    private final int limiteLogin;
    private final int limiteCadastro;
    private final Duration periodo;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(@Value("${app.rate-limit.login}") int limiteLogin,
                           @Value("${app.rate-limit.cadastro}") int limiteCadastro,
                           @Value("${app.rate-limit.janela}") Duration periodo,
                           ObjectMapper objectMapper) {
        this.limiteLogin = limiteLogin;
        this.limiteCadastro = limiteCadastro;
        this.periodo = periodo;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !ROTAS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String rota = request.getRequestURI();
        int limite = LOGIN.equals(rota) ? limiteLogin : limiteCadastro;
        String chave = rota + "|" + request.getRemoteAddr();
        Deque<Instant> tentativas = janelas.computeIfAbsent(chave, ignored -> new ArrayDeque<>());

        synchronized (tentativas) {
            Instant agora = Instant.now();
            Instant limiteInferior = agora.minus(periodo);
            while (!tentativas.isEmpty() && tentativas.peekFirst().isBefore(limiteInferior)) {
                tentativas.removeFirst();
            }
            if (tentativas.size() >= limite) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("Retry-After", String.valueOf(periodo.toSeconds()));
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ErroResponse erro = new ErroResponse(
                        java.time.OffsetDateTime.now(), 429, "LIMITE_EXCEDIDO",
                        "Muitas tentativas. Tente novamente mais tarde", request.getRequestURI(), List.of());
                objectMapper.writeValue(response.getWriter(), erro);
                return;
            }
            tentativas.addLast(agora);
        }
        chain.doFilter(request, response);
    }

    public void limpar() {
        janelas.clear();
    }
}
