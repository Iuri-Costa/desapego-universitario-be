package com.marketplace.desapegoUniversitario.exception;

public class LimiteExcedidoException extends RuntimeException {

    private final long retryAfterSegundos;

    public LimiteExcedidoException(long retryAfterSegundos) {
        super("Muitas tentativas. Tente novamente mais tarde");
        this.retryAfterSegundos = retryAfterSegundos;
    }

    public long getRetryAfterSegundos() {
        return retryAfterSegundos;
    }
}
