package com.marketplace.desapegoUniversitario.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalNotificadorPush implements NotificadorPush {

    private static final Logger log = LoggerFactory.getLogger(LocalNotificadorPush.class);

    @Override
    public boolean enviar(String tokenDispositivo, String titulo, String mensagem) {
        log.debug("Push local ignorado: titulo={}", titulo);
        return true;
    }
}
