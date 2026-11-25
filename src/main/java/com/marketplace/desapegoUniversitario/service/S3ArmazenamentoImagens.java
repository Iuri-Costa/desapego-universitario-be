package com.marketplace.desapegoUniversitario.service;

import com.marketplace.desapegoUniversitario.config.StorageS3Condition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.net.URI;

@Component
@Conditional(StorageS3Condition.class)
public class S3ArmazenamentoImagens implements ArmazenamentoImagens {

    private final S3Client cliente;
    private final String bucket;
    private final String endpoint;
    private final String regiao;
    private final String baseUrlPublica;

    public S3ArmazenamentoImagens(@Value("${app.storage.s3.bucket}") String bucket,
                                  @Value("${app.storage.s3.region}") String regiao,
                                  @Value("${app.storage.s3.endpoint:}") String endpoint,
                                  @Value("${app.storage.s3.access-key:}") String chaveAcesso,
                                  @Value("${app.storage.s3.secret-key:}") String chaveSecreta,
                                  @Value("${app.storage.s3.public-base-url:}") String baseUrlPublica) {
        this.bucket = bucket;
        this.endpoint = endpoint;
        this.regiao = regiao;
        this.baseUrlPublica = baseUrlPublica == null ? "" : baseUrlPublica.trim();

        var construtor = S3Client.builder()
                .region(Region.of(regiao));
        if (!endpoint.isBlank()) {
            construtor.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .chunkedEncodingEnabled(false)
                            .build());
        }
        if (!chaveAcesso.isBlank() && !chaveSecreta.isBlank()) {
            construtor.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(chaveAcesso, chaveSecreta)));
        }
        this.cliente = construtor.build();
    }

    @Override
    public String salvar(String chave, byte[] conteudo, String tipoConteudo) {
        cliente.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .key(chave)
                .contentType(tipoConteudo)
                .build(), RequestBody.fromBytes(conteudo));
        return urlPublica(chave);
    }

    @Override
    public void remover(String chave) {
        cliente.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(chave)
                .build());
    }

    public String urlPublica(String chave) {
        if (!baseUrlPublica.isBlank()) {
            String base = baseUrlPublica.endsWith("/")
                    ? baseUrlPublica.substring(0, baseUrlPublica.length() - 1)
                    : baseUrlPublica;
            return base + "/" + chave;
        }
        String base = endpoint.isBlank()
                ? "https://" + bucket + ".s3." + regiao + ".amazonaws.com"
                : endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) + "/" + bucket
                : endpoint + "/" + bucket;
        return base + "/" + chave;
    }
}
