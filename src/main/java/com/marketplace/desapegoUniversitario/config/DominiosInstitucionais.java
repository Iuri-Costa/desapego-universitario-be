package com.marketplace.desapegoUniversitario.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class DominiosInstitucionais {

    private final List<String> dominios;

    public DominiosInstitucionais(
            @Value("${app.cadastro.dominios-institucionais}") String dominiosConfigurados) {
        this.dominios = Arrays.stream(dominiosConfigurados.split(","))
                .map(String::trim)
                .map(dominio -> dominio.toLowerCase(Locale.ROOT))
                .map(dominio -> dominio.startsWith("@") ? dominio.substring(1) : dominio)
                .map(dominio -> dominio.startsWith(".") ? dominio.substring(1) : dominio)
                .filter(dominio -> !dominio.isEmpty())
                .toList();
        if (dominios.isEmpty()) {
            throw new IllegalStateException(
                    "EMAIL_DOMINIOS_INSTITUCIONAIS deve conter ao menos um domínio");
        }
    }

    public boolean aceito(String email) {
        int posicaoArroba = email == null ? -1 : email.lastIndexOf('@');
        if (posicaoArroba < 0 || posicaoArroba == email.length() - 1) {
            return false;
        }
        String dominio = email.substring(posicaoArroba + 1).toLowerCase(Locale.ROOT);
        return dominios.stream()
                .anyMatch(permitido -> dominio.equals(permitido) || terminaComDominio(dominio, permitido));
    }

    private boolean terminaComDominio(String dominio, String permitido) {
        return dominio.endsWith("." + permitido);
    }
}
