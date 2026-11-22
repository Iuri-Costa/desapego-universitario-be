ALTER TABLE anuncio_imagem ADD COLUMN ordem INTEGER NOT NULL DEFAULT 0;
ALTER TABLE anuncio_imagem ADD COLUMN capa BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE anuncio_imagem i
SET ordem = (
    SELECT COUNT(*)
    FROM anuncio_imagem j
    WHERE j.anuncio_id = i.anuncio_id
      AND (j.criado_em < i.criado_em OR (j.criado_em = i.criado_em AND j.id < i.id))
);

UPDATE anuncio_imagem SET capa = TRUE WHERE ordem = 0;

UPDATE anuncio a
SET imagem = (SELECT i.url FROM anuncio_imagem i WHERE i.anuncio_id = a.id AND i.capa = TRUE);

CREATE INDEX idx_anuncio_imagem_ordem ON anuncio_imagem (anuncio_id, ordem, id);
