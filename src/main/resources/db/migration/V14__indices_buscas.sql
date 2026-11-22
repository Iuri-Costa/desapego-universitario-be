
CREATE INDEX idx_anuncio_listagem ON anuncio (removido_em, criado_em DESC);
CREATE INDEX idx_anuncio_categoria_listagem ON anuncio (categoria, removido_em, criado_em DESC);
CREATE INDEX idx_anuncio_preco_listagem ON anuncio (preco, removido_em);
CREATE INDEX idx_anuncio_titulo_busca ON anuncio (titulo);
CREATE INDEX idx_anuncio_status_listagem ON anuncio (status, removido_em, criado_em DESC);
CREATE INDEX idx_anuncio_usuario_listagem ON anuncio (usuario_id, removido_em);
