package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.service.S3ArmazenamentoImagens;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class S3ArmazenamentoImagensTest {

    @Test
    void deveUsarBasePublicaConfiguradaQuandoDefinida() {
        S3ArmazenamentoImagens armazenamento = new S3ArmazenamentoImagens(
                "bucket-teste", "auto", "https://account.r2.cloudflarestorage.com",
                "", "", "https://cdn.exemplo.com/");

        String url = armazenamento.urlPublica("anuncios/foto.png");

        assertEquals("https://cdn.exemplo.com/anuncios/foto.png", url);
    }

    @Test
    void deveMontarUrlAwsQuandoEndpointNaoInformado() {
        S3ArmazenamentoImagens armazenamento = new S3ArmazenamentoImagens(
                "bucket-teste", "us-east-1", "", "", "", "");

        String url = armazenamento.urlPublica("anuncios/foto.png");

        assertEquals("https://bucket-teste.s3.us-east-1.amazonaws.com/anuncios/foto.png", url);
    }

    @Test
    void deveMontarUrlComEndpointCustomizadoSemBasePublica() {
        S3ArmazenamentoImagens armazenamento = new S3ArmazenamentoImagens(
                "bucket-teste", "auto", "http://localhost:9000", "", "", "  ");

        String url = armazenamento.urlPublica("anuncios/foto.png");

        assertEquals("http://localhost:9000/bucket-teste/anuncios/foto.png", url);
    }
}
