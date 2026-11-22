package com.marketplace.desapegoUniversitario.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    @Conditional(MailHabilitadoCondition.class)
    public JavaMailSender javaMailSender(@Value("${app.mail.host}") String host,
                                         @Value("${app.mail.port}") int port,
                                         @Value("${app.mail.username}") String username,
                                         @Value("${app.mail.password}") String password,
                                         @Value("${app.mail.auth}") boolean auth,
                                         @Value("${app.mail.starttls}") boolean starttls) {
        JavaMailSenderImpl remetente = new JavaMailSenderImpl();
        remetente.setHost(host);
        remetente.setPort(port);
        if (!username.isBlank()) {
            remetente.setUsername(username);
            remetente.setPassword(password);
        }
        Properties propriedades = remetente.getJavaMailProperties();
        propriedades.put("mail.smtp.auth", auth);
        propriedades.put("mail.smtp.starttls.enable", starttls);
        propriedades.put("mail.smtp.timeout", "10000");
        return remetente;
    }
}
