package com.marketplace.desapegoUniversitario.util;

import com.marketplace.desapegoUniversitario.exception.ParametroInvalidoException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class Paginas {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    private Paginas() {
    }

    public static void validar(int page, int size) {
        if (page < 0 || size < 1 || size > TAMANHO_MAXIMO) {
            throw new ParametroInvalidoException(
                    "Use page >= 0 e size entre 1 e " + TAMANHO_MAXIMO);
        }
    }

    public static PageRequest de(int page, int size) {
        validar(page, size);
        return PageRequest.of(page, size);
    }

    public static PageRequest de(int page, int size, Sort ordenacao) {
        validar(page, size);
        return PageRequest.of(page, size, ordenacao);
    }
}
