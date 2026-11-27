-- Bases já existentes podem conter mais de uma reserva ativa por anúncio por causa da
-- corrida entre a checagem em memória e o insert. Mantém a mais recente e expira as demais
-- antes de criar a restrição na migration específica do banco (V20).
UPDATE reserva
SET expirada_em = CURRENT_TIMESTAMP
WHERE expirada_em IS NULL
  AND cancelada_em IS NULL
  AND concluida_em IS NULL
  AND EXISTS (
      SELECT 1
      FROM reserva mais_recente
      WHERE mais_recente.anuncio_id = reserva.anuncio_id
        AND mais_recente.expirada_em IS NULL
        AND mais_recente.cancelada_em IS NULL
        AND mais_recente.concluida_em IS NULL
        AND (mais_recente.criado_em > reserva.criado_em
             OR (mais_recente.criado_em = reserva.criado_em
                 AND mais_recente.id > reserva.id))
  );
