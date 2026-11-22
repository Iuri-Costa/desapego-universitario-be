package com.marketplace.desapegoUniversitario.service;

public interface ArmazenamentoImagens {

    String salvar(String chave, byte[] conteudo, String tipoConteudo);

    void remover(String chave);
}
