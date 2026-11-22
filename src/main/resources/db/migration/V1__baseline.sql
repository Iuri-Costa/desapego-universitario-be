CREATE TABLE usuario (
    id UUID NOT NULL,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE anuncio (
    id UUID NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    preco NUMERIC(38, 2) NOT NULL,
    descricao TEXT,
    imagem TEXT,
    categoria VARCHAR(255) NOT NULL,
    contato TEXT,
    CONSTRAINT pk_anuncio PRIMARY KEY (id)
);
