package com.marketplace.desapegoUniversitario.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.marketplace.desapegoUniversitario.config.MailDesabilitadoCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(MailDesabilitadoCondition.class)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        log.info("Transporte de e-mail não configurado; mensagem não enviada");
    }
}
