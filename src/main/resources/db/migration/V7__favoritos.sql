CREATE TABLE favorito (
    id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    anuncio_id UUID NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_favorito PRIMARY KEY (id),
    CONSTRAINT uk_favorito_usuario_anuncio UNIQUE (usuario_id, anuncio_id),
    CONSTRAINT fk_favorito_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorito_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncio(id) ON DELETE CASCADE
);

CREATE INDEX idx_favorito_usuario ON favorito (usuario_id);
