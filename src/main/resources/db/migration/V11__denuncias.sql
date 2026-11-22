CREATE TABLE denuncia (
    id UUID NOT NULL,
    anuncio_id UUID NOT NULL,
    denunciante_id UUID NOT NULL,
    motivo VARCHAR(50) NOT NULL,
    descricao VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    resolvida_em TIMESTAMP WITH TIME ZONE,
    resolvida_por UUID,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_denuncia PRIMARY KEY (id),
    CONSTRAINT fk_denuncia_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncio(id) ON DELETE CASCADE,
    CONSTRAINT fk_denuncia_denunciante FOREIGN KEY (denunciante_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_denuncia_resolvida_por FOREIGN KEY (resolvida_por) REFERENCES usuario(id)
);

CREATE INDEX idx_denuncia_status ON denuncia (status, criado_em);
