CREATE TABLE mensagem (
    id UUID NOT NULL,
    anuncio_id UUID NOT NULL,
    remetente_id UUID NOT NULL,
    destinatario_id UUID NOT NULL,
    conteudo TEXT NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    lida_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_mensagem PRIMARY KEY (id),
    CONSTRAINT fk_mensagem_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncio(id) ON DELETE CASCADE,
    CONSTRAINT fk_mensagem_remetente FOREIGN KEY (remetente_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_mensagem_destinatario FOREIGN KEY (destinatario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE INDEX idx_mensagem_anuncio ON mensagem (anuncio_id);
CREATE INDEX idx_mensagem_participantes ON mensagem (remetente_id, destinatario_id);
