package com.marketplace.desapegoUniversitario.service;

public interface NotificadorPush {

    void enviar(String tokenDispositivo, String titulo, String mensagem);
}
