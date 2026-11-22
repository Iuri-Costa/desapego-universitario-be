package com.marketplace.desapegoUniversitario;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.storage.max-imagens=2",
        "app.storage.max-tamanho=1KB",
        "app.storage.local.diretorio=${java.io.tmpdir}/desapego-uploads-test"
})
class ImagensAnuncioIntegrationTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFAAH/q842iQAAAABJRU5ErkJggg==");
    private static final byte[] WEBP = concatenated(
            "RIFF".getBytes(), new byte[]{0, 0, 0, 0}, "WEBP".getBytes(), new byte[]{1, 2, 3, 4});
    private static final byte[] JPEG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2};

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveEnviarMultiplasImagensEListarNoAnuncio() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, token);

        mockMvc.perform(multipart("/api/v1/anuncios/{id}/imagens", id)
                        .file(new MockMultipartFile("imagens", "foto.png", "image/png", PNG))
                        .file(new MockMultipartFile("imagens", "foto.webp", "image/webp", WEBP))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].url").value(org.hamcrest.Matchers.startsWith("/uploads/")));

        mockMvc.perform(get("/api/v1/anuncios/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagens.length()").value(2));
    }

    @Test
    void deveRejeitarTipoNaoImagem() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, token);

        mockMvc.perform(multipart("/api/v1/anuncios/{id}/imagens", id)
                        .file(new MockMultipartFile("imagens", "nota.txt", "text/plain",
                                "não é imagem".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("IMAGEM_INVALIDA"));
    }

    @Test
    void deveRejeitarConteudoFalsificado() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, token);

        mockMvc.perform(multipart("/api/v1/anuncios/{id}/imagens", id)
                        .file(new MockMultipartFile("imagens", "falso.png", "image/png",
                                "não é imagem".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("IMAGEM_INVALIDA"));
    }

    @Test
    void deveRejeitarImagemMaiorQueOLimite() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, token);
        byte[] grande = new byte[2048];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);

        mockMvc.perform(multipart("/api/v1/anuncios/{id}/imagens", id)
                        .file(new MockMultipartFile("imagens", "grande.png", "image/png", grande))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("IMAGEM_INVALIDA"));
    }

    @Test
    void deveRespeitarLimiteDeQuantidade() throws Exception {
        String token = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, token);

        mockMvc.perform(multipart("/api/v1/anuncios/{id}/imagens", id)
                        .file(new MockMultipartFile("imagens", "a.png", "image/png", PNG))
                        .file(new MockMultipartFile("imagens", "b.jpg", "image/jpeg", JPEG))
                        .file(new MockMultipartFile("imagens", "c.webp", "image/webp", WEBP))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("IMAGEM_INVALIDA"));
    }

    @Test
    void somenteDonoPodeEnviarERemoverImagens() throws Exception {
        String dono = TestAutenticacao.cadastrarELogar(mockMvc);
        String outro = TestAutenticacao.cadastrarELogar(mockMvc);
        UUID id = TestAutenticacao.criarAnuncio(mockMvc, dono);

        mockMvc.perform(multipart("/api/v1/anuncios/{id}/imagens", id)
                        .file(new MockMultipartFile("imagens", "foto.png", "image/png", PNG))
                        .header("Authorization", "Bearer " + outro))
                .andExpect(status().isForbidden());

        String corpo = mockMvc.perform(multipart("/api/v1/anuncios/{id}/imagens", id)
                        .file(new MockMultipartFile("imagens", "foto.png", "image/png", PNG))
                        .header("Authorization", "Bearer " + dono))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID imagemId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(corpo, "$[0].id"));

        mockMvc.perform(delete("/api/v1/anuncios/{id}/imagens/{imagemId}", id, imagemId)
                        .header("Authorization", "Bearer " + outro))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/anuncios/{id}/imagens/{imagemId}", id, imagemId)
                        .header("Authorization", "Bearer " + dono))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/anuncios/{id}/imagens", id)
                        .header("Authorization", "Bearer " + dono))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private static byte[] concatenated(byte[]... partes) {
        int tamanho = 0;
        for (byte[] parte : partes) {
            tamanho += parte.length;
        }
        byte[] resultado = new byte[tamanho];
        int posicao = 0;
        for (byte[] parte : partes) {
            System.arraycopy(parte, 0, resultado, posicao, parte.length);
            posicao += parte.length;
        }
        return resultado;
    }
}
