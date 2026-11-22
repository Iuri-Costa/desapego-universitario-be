package com.marketplace.desapegoUniversitario.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class SenhaForteValidator implements ConstraintValidator<SenhaForte, String> {

    private static final Pattern FORMATO = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$");

    private static final Set<String> SENHAS_COMUNS = Set.of(
            "senha123", "senha@123", "password", "password123", "12345678", "qwerty123",
            "admin123", "letmein", "welcome1", "abc12345");

    @Override
    public boolean isValid(String senha, ConstraintValidatorContext contexto) {
        if (senha == null) {
            return true;
        }
        return FORMATO.matcher(senha).matches()
                && !SENHAS_COMUNS.contains(senha.toLowerCase(Locale.ROOT));
    }
}
