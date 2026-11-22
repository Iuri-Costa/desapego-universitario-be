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

    public LocalArmazenamentoImagens(@Value("${app.storage.local.diretorio}") String diretorio,
                                     @Value("${app.storage.local.caminho-publico}") String caminhoPublico) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
        this.caminhoPublico = caminhoPublico;
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
            return caminhoPublico + "/" + chave;
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
