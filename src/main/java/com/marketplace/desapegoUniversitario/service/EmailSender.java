package com.marketplace.desapegoUniversitario.service;

public interface EmailSender {

    void enviar(String destinatario, String assunto, String corpo);
}
