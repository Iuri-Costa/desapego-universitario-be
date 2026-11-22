package com.marketplace.desapegoUniversitario.service;

public interface NotificadorPush {

    boolean enviar(String tokenDispositivo, String titulo, String mensagem);
}
