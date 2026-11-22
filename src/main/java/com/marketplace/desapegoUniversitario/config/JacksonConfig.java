package com.marketplace.desapegoUniversitario.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.MapperFeature;

import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer contratoJson() {
        return builder -> builder
                .defaultTimeZone(TimeZone.getTimeZone("UTC"))
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }
}
