UPDATE denuncia
SET motivo = CASE
    WHEN UPPER(motivo) = 'FRAUDE' THEN 'FRAUDE'
    WHEN UPPER(motivo) = 'SPAM' THEN 'SPAM'
    WHEN UPPER(motivo) IN ('DUPLICADO', 'DUPLICATA') THEN 'DUPLICADO'
    WHEN UPPER(motivo) IN ('PRODUTO_PROIBIDO', 'PRODUTO PROIBIDO') THEN 'PRODUTO_PROIBIDO'
    WHEN UPPER(motivo) IN ('OFENSA', 'ABUSO') THEN 'OFENSA'
    ELSE 'OUTROS'
END;

ALTER TABLE denuncia ADD CONSTRAINT ck_denuncia_motivo
    CHECK (motivo IN ('FRAUDE', 'SPAM', 'PRODUTO_PROIBIDO', 'DUPLICADO', 'OFENSA', 'OUTROS'));

CREATE INDEX idx_denuncia_denunciante ON denuncia (denunciante_id, criado_em DESC);
