-- Versao usada nos access tokens: qualquer incremento revoga imediatamente
-- os tokens emitidos antes da mudanca, sem precisar de uma lista de revogacao.
ALTER TABLE usuario ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
