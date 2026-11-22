CREATE TABLE reserva (
    id UUID NOT NULL,
    anuncio_id UUID NOT NULL,
    comprador_id UUID NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    cancelada_em TIMESTAMP WITH TIME ZONE,
    concluida_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_reserva PRIMARY KEY (id),
    CONSTRAINT fk_reserva_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncio(id) ON DELETE CASCADE,
    CONSTRAINT fk_reserva_comprador FOREIGN KEY (comprador_id) REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE INDEX idx_reserva_anuncio ON reserva (anuncio_id);

CREATE TABLE avaliacao (
    id UUID NOT NULL,
    anuncio_id UUID NOT NULL,
    vendedor_id UUID NOT NULL,
    avaliador_id UUID NOT NULL,
    nota INTEGER NOT NULL,
    comentario VARCHAR(1000),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_avaliacao PRIMARY KEY (id),
    CONSTRAINT uk_avaliacao_anuncio_avaliador UNIQUE (anuncio_id, avaliador_id),
    CONSTRAINT fk_avaliacao_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncio(id) ON DELETE CASCADE,
    CONSTRAINT fk_avaliacao_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_avaliacao_avaliador FOREIGN KEY (avaliador_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT ck_avaliacao_nota CHECK (nota BETWEEN 1 AND 5)
);
