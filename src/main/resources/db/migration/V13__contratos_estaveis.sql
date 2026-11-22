
UPDATE usuario SET role = UPPER(role) WHERE role <> UPPER(role);
UPDATE anuncio SET categoria = UPPER(categoria) WHERE categoria <> UPPER(categoria);
UPDATE anuncio SET tipo = UPPER(tipo) WHERE tipo <> UPPER(tipo);
UPDATE anuncio SET condicao = UPPER(condicao) WHERE condicao <> UPPER(condicao);
UPDATE anuncio SET status = UPPER(status) WHERE status <> UPPER(status);
UPDATE usuario_token SET tipo = UPPER(tipo) WHERE tipo <> UPPER(tipo);
UPDATE denuncia SET status = UPPER(status) WHERE status <> UPPER(status);

ALTER TABLE usuario ADD CONSTRAINT ck_usuario_role
    CHECK (role IN ('USUARIO', 'ADMIN'));

ALTER TABLE anuncio ADD CONSTRAINT ck_anuncio_categoria
    CHECK (categoria IN ('LIVROS', 'ELETRONICOS', 'ENGENHARIA', 'COMPUTACAO',
                         'MOVEIS', 'VESTUARIO', 'PAPELARIA', 'OUTROS'));
ALTER TABLE anuncio ADD CONSTRAINT ck_anuncio_tipo
    CHECK (tipo IN ('VENDA', 'DOACAO'));
ALTER TABLE anuncio ADD CONSTRAINT ck_anuncio_condicao
    CHECK (condicao IN ('NOVO', 'SEMINOVO', 'USADO'));
ALTER TABLE anuncio ADD CONSTRAINT ck_anuncio_status
    CHECK (status IN ('ATIVO', 'RESERVADO', 'VENDIDO', 'DOADO', 'REMOVIDO'));

ALTER TABLE usuario_token ADD CONSTRAINT ck_usuario_token_tipo
    CHECK (tipo IN ('VERIFICACAO_EMAIL', 'REDEFINICAO_SENHA'));

ALTER TABLE denuncia ADD CONSTRAINT ck_denuncia_status
    CHECK (status IN ('PENDENTE', 'RESOLVIDA', 'REJEITADA'));
