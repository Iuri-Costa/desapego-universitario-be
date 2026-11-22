CREATE TABLE notificacao (
    id UUID NOT NULL,
    destinatario_id UUID NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensagem VARCHAR(500) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    referencia_id UUID,
    lida_em TIMESTAMP WITH TIME ZONE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_notificacao PRIMARY KEY (id),
    CONSTRAINT fk_notificacao_destinatario FOREIGN KEY (destinatario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE INDEX idx_notificacao_destinatario ON notificacao (destinatario_id, criado_em DESC);

CREATE TABLE dispositivo_fcm (
    id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    token VARCHAR(512) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_dispositivo_fcm PRIMARY KEY (id),
    CONSTRAINT uk_dispositivo_fcm_token UNIQUE (token),
    CONSTRAINT fk_dispositivo_fcm_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE INDEX idx_dispositivo_fcm_usuario ON dispositivo_fcm (usuario_id);
