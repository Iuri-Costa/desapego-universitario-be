package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.config.MailHabilitadoCondition;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Conditional(MailHabilitadoCondition.class)
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String remetente;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${app.mail.from}") String remetente,
                           @Value("${app.mail.username}") String usuario) {
        this.mailSender = mailSender;
        this.remetente = remetente.isBlank() ? usuario : remetente;
    }

    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpo, true);
            mailSender.send(mensagem);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao enviar e-mail de transação", e);
        }
    }
}
