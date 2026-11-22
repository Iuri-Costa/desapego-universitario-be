package com.marketplace.desapegoUniversitario.security;

import com.marketplace.desapegoUniversitario.exception.LimiteExcedidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimitadorRequisicoes {

    public enum Tipo { LOGIN, CADASTRO }

    private final Map<String, Deque<Instant>> janelas = new ConcurrentHashMap<>();
    private final int limiteLogin;
    private final int limiteCadastro;
    private final Duration janela;

    public LimitadorRequisicoes(@Value("${app.rate-limit.login}") int limiteLogin,
                                @Value("${app.rate-limit.cadastro}") int limiteCadastro,
                                @Value("${app.rate-limit.janela}") Duration janela) {
        this.limiteLogin = limiteLogin;
        this.limiteCadastro = limiteCadastro;
        this.janela = janela;
    }

    public void verificarIp(Tipo tipo, String ip) {
        verificar("ip|" + tipo + "|" + ip, limite(tipo));
    }

    public void verificarEmail(Tipo tipo, String email) {
        String normalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        verificar("email|" + tipo + "|" + normalizado, limite(tipo));
    }

    public void limpar() {
        janelas.clear();
    }

    private int limite(Tipo tipo) {
        return tipo == Tipo.LOGIN ? limiteLogin : limiteCadastro;
    }

    private void verificar(String chave, int limite) {
        Deque<Instant> tentativas = janelas.computeIfAbsent(chave, ignored -> new ArrayDeque<>());
        synchronized (tentativas) {
            Instant agora = Instant.now();
            Instant limiteInferior = agora.minus(janela);
            while (!tentativas.isEmpty() && tentativas.peekFirst().isBefore(limiteInferior)) {
                tentativas.removeFirst();
            }
            if (tentativas.size() >= limite) {
                throw new LimiteExcedidoException(janela.toSeconds());
            }
            tentativas.addLast(agora);
        }
    }
}
