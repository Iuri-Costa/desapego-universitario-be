ALTER TABLE usuario ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USUARIO';

ALTER TABLE anuncio ADD COLUMN usuario_id UUID;
ALTER TABLE anuncio ADD COLUMN moderado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE anuncio ADD CONSTRAINT fk_anuncio_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE SET NULL;

CREATE INDEX idx_anuncio_usuario ON anuncio (usuario_id);
