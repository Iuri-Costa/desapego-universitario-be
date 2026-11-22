package com.marketplace.desapegoUniversitario.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class FcmCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String credenciais = context.getEnvironment().getProperty("app.fcm.credentials");
        return credenciais != null && !credenciais.isBlank();
    }
}
