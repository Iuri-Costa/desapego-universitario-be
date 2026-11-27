-- Coluna gerada: só é preenchida para reservas ativas, permitindo unicidade sem índice parcial.
ALTER TABLE reserva ADD COLUMN anuncio_ativa_id UUID
    GENERATED ALWAYS AS (CASE
        WHEN cancelada_em IS NULL AND concluida_em IS NULL AND expirada_em IS NULL
        THEN anuncio_id END);

CREATE UNIQUE INDEX uk_reserva_ativa_por_anuncio ON reserva (anuncio_ativa_id);
