package com.marketplace.desapegoUniversitario;

import com.marketplace.desapegoUniversitario.service.LocalArmazenamentoImagens;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalArmazenamentoImagensTest {

    @Test
    void deveDevolverUrlRelativaQuandoNaoHaBasePublica() throws Exception {
        Path diretorio = Files.createTempDirectory("desapego-local-relativa");
        var storage = new LocalArmazenamentoImagens(diretorio.toString(), "/uploads", "");

        String url = storage.salvar("anuncios/foto.png", new byte[]{1, 2, 3}, "image/png");

        assertEquals("/uploads/anuncios/foto.png", url);
        assertTrue(Files.exists(diretorio.resolve("anuncios/foto.png")));
    }

    @Test
    void deveDevolverUrlAbsolutaQuandoBasePublicaConfigurada() throws Exception {
        Path diretorio = Files.createTempDirectory("desapego-local-absoluta");
        var storage = new LocalArmazenamentoImagens(
                diretorio.toString(), "/uploads", "https://api.example.com/");

        String url = storage.salvar("anuncios/foto.png", new byte[]{1, 2, 3}, "image/png");

        assertEquals("https://api.example.com/uploads/anuncios/foto.png", url);
        assertTrue(Files.exists(diretorio.resolve("anuncios/foto.png")));
    }
}
