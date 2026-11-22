ALTER TABLE usuario ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE usuario_token (
    id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    usado_em TIMESTAMP WITH TIME ZONE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_usuario_token PRIMARY KEY (id),
    CONSTRAINT fk_usuario_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT uk_usuario_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_usuario_token_usuario_tipo ON usuario_token (usuario_id, tipo);
