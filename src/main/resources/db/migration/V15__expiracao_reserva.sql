
ALTER TABLE reserva ADD COLUMN expirada_em TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_reserva_expiracao
    ON reserva (cancelada_em, concluida_em, expirada_em, criado_em);
