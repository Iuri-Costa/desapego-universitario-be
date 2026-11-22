package com.marketplace.desapegoUniversitario.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class StorageS3Condition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String bucket = context.getEnvironment().getProperty("app.storage.s3.bucket");
        return bucket != null && !bucket.isBlank();
    }
}
