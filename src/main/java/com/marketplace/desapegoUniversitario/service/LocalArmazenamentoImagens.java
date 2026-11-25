package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.config.StorageLocalCondition;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Conditional(StorageLocalCondition.class)
public class LocalArmazenamentoImagens implements ArmazenamentoImagens {

    private final Path diretorio;
    private final String caminhoPublico;
    private final String basePublica;

    public LocalArmazenamentoImagens(@Value("${app.storage.local.diretorio}") String diretorio,
                                     @Value("${app.storage.local.caminho-publico}") String caminhoPublico,
                                     @Value("${app.storage.url-base:}") String basePublica) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
        this.caminhoPublico = caminhoPublico;
        this.basePublica = basePublica == null ? "" : basePublica.trim();
    }

    @PostConstruct
    void criarDiretorio() throws IOException {
        Files.createDirectories(diretorio);
    }

    @Override
    public String salvar(String chave, byte[] conteudo, String tipoConteudo) {
        try {
            Path destino = resolver(chave);
            Path pai = destino.getParent();
            if (pai != null) {
                Files.createDirectories(pai);
            }
            Files.write(destino, conteudo);
            String relativa = caminhoPublico + "/" + chave;
            if (basePublica.isEmpty()) {
                return relativa;
            }
            return basePublica.endsWith("/")
                    ? basePublica.substring(0, basePublica.length() - 1) + relativa
                    : basePublica + relativa;
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao armazenar imagem", e);
        }
    }

    @Override
    public void remover(String chave) {
        try {
            Files.deleteIfExists(resolver(chave));
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao remover imagem", e);
        }
    }

    private Path resolver(String chave) {
        Path destino = diretorio.resolve(chave).normalize();
        if (!destino.startsWith(diretorio)) {
            throw new IllegalArgumentException("Chave de armazenamento inválida");
        }
        return destino;
    }
}
