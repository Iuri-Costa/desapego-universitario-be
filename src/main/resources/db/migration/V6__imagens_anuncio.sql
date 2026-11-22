CREATE TABLE anuncio_imagem (
    id UUID NOT NULL,
    anuncio_id UUID NOT NULL,
    chave VARCHAR(500) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    tipo_conteudo VARCHAR(100) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_anuncio_imagem PRIMARY KEY (id),
    CONSTRAINT uk_anuncio_imagem_chave UNIQUE (chave),
    CONSTRAINT fk_anuncio_imagem_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncio(id) ON DELETE CASCADE
);

CREATE INDEX idx_anuncio_imagem_anuncio ON anuncio_imagem (anuncio_id);
