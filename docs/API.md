# API — Desapego Universitário

Última revisão: 2026-11-22. Este documento descreve o código atual do backend.
O contrato gerado automaticamente está em `/v3/api-docs` (alias legado: `/openapi.json`) e a UI em `/swagger-ui.html`.
Todas as rotas de negócio usam o prefixo `/api/v1`; rotas de infraestrutura estão listadas no final.
[Política de versionamento](VERSIONAMENTO.md).

## Convenções

- Identificadores são UUID (`11111111-1111-1111-1111-111111111111` nos exemplos).
- Datas em ISO-8601 UTC (`2026-11-22T16:00:00Z`).
- Valores monetários são JSON number com até 2 casas decimais (`80.00`).
- Enums são upper-case.
- Toda resposta de erro usa `ErroResponse`, inclusive 401/403 dos filtros de segurança e 429 do rate limit.
- `page` começa em 0; `size` vai de 1 a 100 (padrão 20).
- `sort` usa `campo,direcao`, separado por `;` para múltiplos campos. Ex.: `preco,asc;criadoEm,desc`.
- Campos permitidos em `sort`: `titulo`, `preco`, `categoria`, `condicao`, `tipo`, `status`, `campus`, `criadoEm`, `atualizadoEm`.

Exemplos abaixo usam:

```bash
BASE=https://seu-host/api/v1
TOKEN=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbHVub0B1bml2ZXJzaWRhZGUuZWR1LmJyIn0.exemplo
AUTH="Authorization: Bearer $TOKEN"
ID=11111111-1111-1111-1111-111111111111
```

## Autenticação, CORS e correlação

- Envie `Authorization: Bearer <accessToken>` nas rotas protegidas.
- Access token: 15 min (`JWT_ACCESS_TOKEN_TTL`). Refresh token: 7 dias (`JWT_REFRESH_TOKEN_TTL`), de uso único; a rotação revoga a família inteira se o token antigo for reutilizado.
- Algoritmo: HS256 (`app.jwt.secret` / `JWT_SECRET`, mínimo 32 bytes).
- CORS aceita apenas origens explícitas de `CORS_ALLOWED_ORIGINS`; não usa credenciais/cookies.
  - Headers permitidos: `Authorization`, `Content-Type`, `X-Correlation-Id`.
  - Headers expostos ao navegador: `Retry-After`, `X-Correlation-Id`.
- Toda requisição recebe `X-Correlation-Id`. Se o cliente enviar um valor válido (até 64 caracteres, `[A-Za-z0-9._-]`), ele é preservado; caso contrário um UUID é gerado. O mesmo valor entra no campo `correlationId` dos erros e nos logs.

## Erros

```json
{
  "timestamp": "2026-11-22T16:00:00Z",
  "status": 429,
  "codigo": "LIMITE_EXCEDIDO",
  "mensagem": "Muitas tentativas. Tente novamente mais tarde",
  "caminho": "/api/v1/auth/login",
  "correlationId": "9f1c2f7e-6b0a-4f2e-9c66-3a1f0e2d4b11",
  "erros": []
}
```

Códigos estáveis de `codigo`: `VALIDACAO`, `REQUISICAO_INVALIDA`, `PARAMETRO_INVALIDO`, `PARAMETRO_OBRIGATORIO`, `IMAGEM_INVALIDA`, `NAO_ENCONTRADO`, `LIMITE_EXCEDIDO`, `CREDENCIAIS_INVALIDAS`, `TOKEN_INVALIDO`, `NAO_AUTENTICADO`, `ACESSO_NEGADO`, `DOMINIO_INVALIDO`, `CONFLITO`, `CONFLITO_DE_DADOS`, `METODO_NAO_SUPORTADO`, `ERRO_INTERNO`.

Erros de validação preenchem `erros`, por exemplo:

```json
{
  "timestamp": "2026-11-22T16:00:00Z",
  "status": 400,
  "codigo": "VALIDACAO",
  "mensagem": "Erro de validação",
  "caminho": "/api/v1/anuncios",
  "correlationId": "9f1c2f7e-6b0a-4f2e-9c66-3a1f0e2d4b11",
  "erros": [
    { "campo": "titulo", "mensagem": "O título é obrigatório" }
  ]
}
```

## Rate limiting

Login e cadastro usam janela deslizante por IP **e** por e-mail. As rotas cobertas são:

- `POST /api/v1/auth/login`
- `POST /api/v1/usuarios/login` (alias legado)
- `POST /api/v1/usuarios/cadastro`

Configuração: `RATE_LIMIT_LOGIN` (padrão 5), `RATE_LIMIT_CADASTRO` (padrão 3) e `RATE_LIMIT_JANELA` (padrão `PT1M`). Quando excedido, a resposta é `429` com `ErroResponse`, `codigo = LIMITE_EXCEDIDO` e header `Retry-After` em segundos.

O limite por IP usa `request.getRemoteAddr()`; `X-Forwarded-For` fornecido pelo cliente não é aceito. Os contadores ficam em memória, por instância da aplicação — atrás de balanceador, cada réplica tem seu próprio contador.

## Cadastro, verificação e papéis

- O cadastro aceita somente domínios configurados em `EMAIL_DOMINIOS_INSTITUCIONAIS` (padrão `edu.br,edu,ac.br`).
- Para evitar enumeração, `POST /cadastro` sempre responde `200` com a mesma mensagem, mesmo para e-mail já cadastrado.
- Login exige e-mail verificado.
- Roles: `USUARIO` (padrão) e `ADMIN`.
- Não existe endpoint público para criar/promover ADMIN. Defina `ADMIN_BOOTSTRAP_EMAIL` com o e-mail de uma conta **já cadastrada e verificada** e reinicie a aplicação: a promoção ocorre somente na inicialização. Rotas administrativas também têm `@PreAuthorize("hasRole('ADMIN')")`.

## Paginação

Envelope retornado por buscas paginadas:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

## Enums

- `Categoria`: `LIVROS`, `ELETRONICOS`, `ENGENHARIA`, `COMPUTACAO`, `MOVEIS`, `VESTUARIO`, `PAPELARIA`, `OUTROS`
- `TipoAnuncio`: `VENDA`, `DOACAO`
- `Condicao`: `NOVO`, `SEMINOVO`, `USADO`
- `StatusAnuncio`: `ATIVO`, `RESERVADO`, `VENDIDO`, `DOADO`, `REMOVIDO`
- `StatusReserva`: `ATIVA`, `CANCELADA`, `CONCLUIDA`, `EXPIRADA`
- `StatusDenuncia`: `PENDENTE`, `RESOLVIDA`, `REJEITADA`

---

# Endpoints

## Autenticação (`/auth`)

| Método | Rota | Auth | Exemplo | Resposta |
|---|---|---|---|---|
| POST | `/auth/login` | Pública, limitada | `curl -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"email":"aluno@universidade.edu.br","senha":"Senha@12345"}'` | `200` `TokenResponseDTO` |
| POST | `/auth/refresh` | Pública | `curl -X POST "$BASE/auth/refresh" -H 'Content-Type: application/json' -d '{"refreshToken":"<refresh>"}'` | `200` novos tokens; `401` se inválido/reutilizado |
| POST | `/auth/logout` | Bearer | `curl -X POST "$BASE/auth/logout" -H "$AUTH"` | `204`; revoga a família de refresh tokens |

`TokenResponseDTO`:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbHVub0B1bml2ZXJzaWRhZGUuZWR1LmJyIn0.exemplo",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJ0aXBvIjoicmVmcmVzaCJ9.exemplo",
  "tipo": "Bearer",
  "expiresIn": 900
}
```

## Usuários (`/usuarios`)

| Método | Rota | Auth | Exemplo | Resposta |
|---|---|---|---|---|
| POST | `/usuarios/cadastro` | Pública, limitada | `curl -X POST "$BASE/usuarios/cadastro" -H 'Content-Type: application/json' -d '{"nome":"Maria Almeida","email":"maria@universidade.edu.br","senha":"Senha@12345"}'` | `200` `{"mensagem":"Se o e-mail for elegível, enviamos instruções para concluir o cadastro"}` |
| POST | `/usuarios/verificar-email` | Pública | `curl -X POST "$BASE/usuarios/verificar-email" -H 'Content-Type: application/json' -d '{"token":"<token do e-mail>"}'` | `204` |
| POST | `/usuarios/reenviar-verificacao` | Pública | `curl -X POST "$BASE/usuarios/reenviar-verificacao" -H 'Content-Type: application/json' -d '{"email":"maria@universidade.edu.br"}'` | `202` |
| POST | `/usuarios/recuperar-senha` | Pública | `curl -X POST "$BASE/usuarios/recuperar-senha" -H 'Content-Type: application/json' -d '{"email":"maria@universidade.edu.br"}'` | `202` |
| POST | `/usuarios/redefinir-senha` | Pública | `curl -X POST "$BASE/usuarios/redefinir-senha" -H 'Content-Type: application/json' -d '{"token":"<token>","novaSenha":"Nova@Senha123"}'` | `204` |
| POST | `/usuarios/login` | Pública, limitada | `curl -X POST "$BASE/usuarios/login" -H 'Content-Type: application/json' -d '{"email":"aluno@universidade.edu.br","senha":"Senha@12345"}'` | `200` `TokenResponseDTO` (alias legado de `/auth/login`) |

## Perfil (`/usuarios/me`)

| Método | Rota | Auth | Exemplo | Resposta |
|---|---|---|---|---|
| GET | `/usuarios/me` | Bearer | `curl "$BASE/usuarios/me" -H "$AUTH"` | `200` `PerfilResponseDTO` |
| PATCH | `/usuarios/me` | Bearer | `curl -X PATCH "$BASE/usuarios/me" -H "$AUTH" -H 'Content-Type: application/json' -d '{"nome":"Maria Almeida"}'` | `200` perfil atualizado (somente `nome` é editável) |
| GET | `/usuarios/me/anuncios` | Bearer | `curl "$BASE/usuarios/me/anuncios?status=ATIVO&incluirRemovidos=false&page=0&size=20" -H "$AUTH"` | `200` `PaginadoDTO<AnuncioResponseDTO>` |

`PerfilResponseDTO`:

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "nome": "Maria Almeida",
  "email": "maria@universidade.edu.br",
  "role": "USUARIO",
  "emailVerificado": true,
  "criadoEm": "2026-11-22T16:00:00Z",
  "totalAnuncios": 3,
  "anunciosAtivos": 2,
  "favoritos": 1
}
```

## Anúncios (`/anuncios`)

| Método | Rota | Auth | Exemplo | Resposta |
|---|---|---|---|---|
| POST | `/anuncios` | Bearer | `curl -X POST "$BASE/anuncios" -H "$AUTH" -H 'Content-Type: application/json' -d '{"titulo":"Calculadora científica","preco":80.00,"descricao":"Pouco usada","categoria":"ELETRONICOS","tipo":"VENDA","condicao":"USADO","campus":"PRINCIPAL","localRetirada":"A combinar","contato":"WhatsApp 99999-9999"}'` | `200` `AnuncioResponseDTO`, status inicial `ATIVO` |
| GET | `/anuncios` | Pública | `curl "$BASE/anuncios?q=calculadora&categoria=ELETRONICOS&precoMin=10&precoMax=100&page=0&size=20&sort=preco,asc;criadoEm,desc"` | `200` `PaginadoDTO<AnuncioResponseDTO>` |
| GET | `/anuncios/{id}` | Pública | `curl "$BASE/anuncios/$ID"` | `200` `AnuncioResponseDTO`; `404` se removido |
| GET | `/anuncios/categoria/{categoria}` | Pública | `curl "$BASE/anuncios/categoria/LIVROS"` | `200` lista de `AnuncioResponseDTO` |
| PUT | `/anuncios/{id}` | Bearer, dono | `curl -X PUT "$BASE/anuncios/$ID" -H "$AUTH" -H 'Content-Type: application/json' -d '{"titulo":"Calculadora","preco":70.00,"categoria":"ELETRONICOS"}'` | `200` anúncio atualizado; `403` se não for dono |
| PATCH | `/anuncios/{id}` | Bearer, dono | `curl -X PATCH "$BASE/anuncios/$ID" -H "$AUTH" -H 'Content-Type: application/json' -d '{"preco":65.00}'` | `200` anúncio atualizado |
| DELETE | `/anuncios/{id}` | Bearer, dono | `curl -X DELETE "$BASE/anuncios/$ID" -H "$AUTH"` | `204`; soft delete (`status=REMOVIDO`) |
| POST | `/anuncios/{id}/imagens` | Bearer, dono | `curl -X POST "$BASE/anuncios/$ID/imagens" -H "$AUTH" -F 'imagens=@foto1.jpg' -F 'imagens=@foto2.png'` | `200` `[{"id":"...","url":"/uploads/anuncios/..."}]` |
| GET | `/anuncios/{id}/imagens` | Bearer, dono | `curl "$BASE/anuncios/$ID/imagens" -H "$AUTH"` | `200` lista de `ImagemResponseDTO` |
| DELETE | `/anuncios/{id}/imagens/{imagemId}` | Bearer, dono | `curl -X DELETE "$BASE/anuncios/$ID/imagens/22222222-2222-2222-2222-222222222222" -H "$AUTH"` | `204` |

Defaults na criação: `tipo=VENDA`, `condicao=USADO`, `campus=PRINCIPAL`, `localRetirada="A combinar"`. Imagens aceitas: JPEG/PNG/WebP, até `STORAGE_MAX_IMAGENS` (padrão 5) por anúncio e `STORAGE_MAX_TAMANHO` (padrão 2MB) por arquivo; magic bytes são validados. O campo legado `imagem` não é sincronizado com o upload.

`AnuncioResponseDTO` (resumo):

```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "titulo": "Calculadora científica",
  "preco": 80.00,
  "descricao": "Pouco usada",
  "imagem": null,
  "categoria": "ELETRONICOS",
  "contato": "WhatsApp 99999-9999",
  "moderado": false,
  "tipo": "VENDA",
  "condicao": "USADO",
  "status": "ATIVO",
  "campus": "PRINCIPAL",
  "localRetirada": "A combinar",
  "criadoEm": "2026-11-22T16:00:00Z",
  "atualizadoEm": "2026-11-22T16:00:00Z",
  "imagens": []
}
```

## Favoritos

| Método | Rota | Auth | Exemplo | Resposta |
|---|---|---|---|---|
| POST | `/anuncios/{id}/favoritos` | Bearer | `curl -X POST "$BASE/anuncios/$ID/favoritos" -H "$AUTH"` | `200` `{"anuncioId":"...","favorito":true}`; `400` no próprio anúncio; `409` se já favoritado |
| DELETE | `/anuncios/{id}/favoritos` | Bearer | `curl -X DELETE "$BASE/anuncios/$ID/favoritos" -H "$AUTH"` | `204` |
| GET | `/usuarios/me/favoritos` | Bearer | `curl "$BASE/usuarios/me/favoritos?page=0&size=20" -H "$AUTH"` | `200` `PaginadoDTO<AnuncioResponseDTO>` |

Não é possível favoritar o próprio anúncio (`400`, `codigo=PARAMETRO_INVALIDO`). Favorito duplicado retorna `409`, `codigo=CONFLITO`. Não há endpoint para consultar o estado de um favorito específico.

## Mensagens

| Método | Rota | Auth | Exemplo | Resposta |
|---|---|---|---|---|
| POST | `/anuncios/{id}/mensagens` | Bearer | `curl -X POST "$BASE/anuncios/$ID/mensagens" -H "$AUTH" -H 'Content-Type: application/json' -d '{"conteudo":"Ainda está disponível?"}'` | `201` `MensagemResponseDTO` |
| GET | `/anuncios/{id}/mensagens` | Bearer | `curl "$BASE/anuncios/$ID/mensagens?compradorId=33333333-3333-3333-3333-333333333333" -H "$AUTH"` | `200` lista de mensagens da conversa |
| GET | `/mensagens/conversas` (alias: `/usuarios/me/conversas`) | Bearer | `curl "$BASE/mensagens/conversas" -H "$AUTH"` | `200` lista de `ConversaResponseDTO`, ordenada pela última mensagem |
| PATCH | `/mensagens/{id}/lida` | Bearer, destinatário | `curl -X PATCH "$BASE/mensagens/44444444-4444-4444-4444-444444444444/lida" -H "$AUTH"` | `200` mensagem com `lidaEm` preenchido |

Mensagens só trafegam entre o vendedor e o comprador de uma reserva ativa ou concluída do anúncio; terceiros recebem `403` (`codigo=ACESSO_NEGADO`) no envio e na listagem. `destinatarioId` no POST é obrigatório apenas quando o remetente é o vendedor e deve ser o comprador da reserva; quando o remetente é o comprador, a mensagem vai para o vendedor. O histórico de mensagens e a listagem de conversas não são paginados.

`MensagemResponseDTO`:

```json
{
  "id": "44444444-4444-4444-4444-444444444444",
  "anuncioId": "11111111-1111-1111-1111-111111111111",
  "remetenteId": "33333333-3333-3333-3333-333333333333",
  "remetenteNome": "João Silva",
  "destinatarioId": "55555555-5555-5555-5555-555555555555",
  "conteudo": "Ainda está disponível?",
  "criadoEm": "2026-11-22T16:00:00Z",
  "lidaEm": null
}
```

`ConversaResponseDTO`:

```json
{
  "anuncioId": "11111111-1111-1111-1111-111111111111",
  "anuncioTitulo": "Calculadora científica",
  "anuncioStatus": "RESERVADO",
  "compradorId": "33333333-3333-3333-3333-333333333333",
  "vendedorId": "55555555-5555-5555-5555-555555555555",
  "outroUsuarioId": "33333333-3333-3333-3333-333333333333",
  "outroUsuarioNome": "João Silva",
  "ultimaMensagem": "Ainda está disponível?",
  "ultimaMensagemEm": "2026-11-22T16:00:00Z",
  "mensagensNaoLidas": 1
}
```

## Reservas, conclusão e avaliações

| Método | Rota | Auth | Exemplo | Resposta |
|---|---|---|---|---|
| POST | `/anuncios/{id}/reserva` | Bearer, comprador | `curl -X POST "$BASE/anuncios/$ID/reserva" -H "$AUTH"` | `200` anúncio com `status=RESERVADO`; `409` se já reservado por outro comprador |
| GET | `/anuncios/{id}/reserva` | Bearer, comprador/vendedor | `curl "$BASE/anuncios/$ID/reserva" -H "$AUTH"` | `200` `ReservaResponseDTO`; `403` para terceiros; `404` sem reserva |
| GET | `/usuarios/me/reservas` | Bearer | `curl "$BASE/usuarios/me/reservas?page=0&size=20" -H "$AUTH"` | `200` `PaginadoDTO<ReservaResponseDTO>` com o histórico do usuário |
| DELETE | `/anuncios/{id}/reserva` | Bearer, comprador/vendedor | `curl -X DELETE "$BASE/anuncios/$ID/reserva" -H "$AUTH"` | `200` anúncio volta para `ATIVO` |
| POST | `/anuncios/{id}/conclusao` | Bearer, vendedor | `curl -X POST "$BASE/anuncios/$ID/conclusao" -H "$AUTH"` | `200` `VENDIDO` ou `DOADO` conforme `tipo` |
| POST | `/anuncios/{id}/avaliacoes` | Bearer, comprador da reserva concluída | `curl -X POST "$BASE/anuncios/$ID/avaliacoes" -H "$AUTH" -H 'Content-Type: application/json' -d '{"nota":5,"comentario":"Transação tranquila"}'` | `200` `AvaliacaoResponseDTO` (upsert por anúncio/avaliador) |
| GET | `/usuarios/{id}/avaliacoes` | Bearer | `curl "$BASE/usuarios/55555555-5555-5555-5555-555555555555/avaliacoes" -H "$AUTH"` | `200` `{"media":4.5,"total":2,"avaliacoes":[...]}` |

Uma reserva ativa expira automaticamente após `RESERVA_TTL` (padrão `PT24H`): o anúncio volta para `ATIVO` e a reserva passa a ter `status=EXPIRADA`. A expiração é aplicada ao consultar uma reserva, listar o histórico, criar/cancelar/concluir uma reserva ou trocar mensagens. `ReservaResponseDTO.status` é `ATIVA`, `CANCELADA`, `CONCLUIDA` ou `EXPIRADA`. A avaliação é apenas comprador → vendedor.

`ReservaResponseDTO`:

```json
{
  "id": "77777777-7777-7777-7777-777777777777",
  "anuncioId": "11111111-1111-1111-1111-111111111111",
  "anuncioTitulo": "Calculadora científica",
  "anuncioStatus": "RESERVADO",
  "compradorId": "33333333-3333-3333-3333-333333333333",
  "compradorNome": "João Silva",
  "vendedorId": "55555555-5555-5555-5555-555555555555",
  "status": "ATIVA",
  "criadoEm": "2026-11-22T16:00:00Z",
  "canceladaEm": null,
  "concluidaEm": null,
  "expiradaEm": null
}
```

## Denúncias

| Método | Rota | Auth | Exemplo | Resposta |
|---|---|---|---|---|
| POST | `/anuncios/{id}/denuncias` | Bearer | `curl -X POST "$BASE/anuncios/$ID/denuncias" -H "$AUTH" -H 'Content-Type: application/json' -d '{"motivo":"Fraude","descricao":"Anúncio duplicado suspeito"}'` | `201` `DenunciaResponseDTO` com `status=PENDENTE` |

Não denunciar próprio anúncio (`400`); duplicata pendente retorna `409`. `motivo` é texto livre (até 50 caracteres). Não há listagem de denúncias pelo denunciante; a listagem/resolução é administrativa.

## Notificações e FCM

| Método | Rota | Auth | Exemplo | Resposta |
|---|---|---|---|---|
| GET | `/notificacoes` | Bearer | `curl "$BASE/notificacoes?page=0&size=20" -H "$AUTH"` | `200` `PaginadoDTO<NotificacaoResponseDTO>` |
| GET | `/notificacoes/nao-lidas` | Bearer | `curl "$BASE/notificacoes/nao-lidas" -H "$AUTH"` | `200` `{"total":3}` |
| PATCH | `/notificacoes/{id}/lida` | Bearer, destinatário | `curl -X PATCH "$BASE/notificacoes/66666666-6666-6666-6666-666666666666/lida" -H "$AUTH"` | `200` notificação com `lidaEm` |
| POST | `/notificacoes/dispositivo` | Bearer | `curl -X POST "$BASE/notificacoes/dispositivo" -H "$AUTH" -H 'Content-Type: application/json' -d '{"token":"token-fcm-do-dispositivo"}'` | `201` vazio |
| DELETE | `/notificacoes/dispositivo` | Bearer | `curl -X DELETE "$BASE/notificacoes/dispositivo" -H "$AUTH" -H 'Content-Type: application/json' -d '{"token":"token-fcm-do-dispositivo"}'` | `204` |

Tipos produzidos: `MENSAGEM`, `RESERVA`, `CONCLUSAO`, `AVALIACAO`, `DENUNCIA`. O token FCM nunca é devolvido pela API. Sem `FCM_CREDENTIALS`, as notificações ficam apenas no banco (`LocalNotificadorPush`).

`NotificacaoResponseDTO`:

```json
{
  "id": "66666666-6666-6666-6666-666666666666",
  "titulo": "Nova mensagem",
  "mensagem": "Você recebeu uma mensagem sobre um anúncio",
  "tipo": "MENSAGEM",
  "referenciaId": "11111111-1111-1111-1111-111111111111",
  "criadoEm": "2026-11-22T16:00:00Z",
  "lidaEm": null
}
```

## Administração (`/admin`, somente ADMIN)

Todas as rotas exigem `ROLE_ADMIN` no filtro HTTP e também `@PreAuthorize("hasRole('ADMIN')")`.

| Método | Rota | Exemplo | Resposta |
|---|---|---|---|
| PATCH | `/admin/anuncios/{id}/moderacao` | `curl -X PATCH "$BASE/admin/anuncios/$ID/moderacao" -H "$AUTH" -H 'Content-Type: application/json' -d '{"moderado":true}'` | `200` anúncio com flag `moderado` |
| DELETE | `/admin/anuncios/{id}` | `curl -X DELETE "$BASE/admin/anuncios/$ID" -H "$AUTH"` | `204`; soft delete por moderação |
| GET | `/admin/denuncias` | `curl "$BASE/admin/denuncias?status=PENDENTE&page=0&size=20" -H "$AUTH"` | `200` `PaginadoDTO<DenunciaResponseDTO>` |
| POST | `/admin/denuncias/{id}/resolucao` | `curl -X POST "$BASE/admin/denuncias/77777777-7777-7777-7777-777777777777/resolucao" -H "$AUTH" -H 'Content-Type: application/json' -d '{"status":"RESOLVIDA","removerAnuncio":true}'` | `200` denúncia resolvida |

`DenunciaResponseDTO`:

```json
{
  "id": "77777777-7777-7777-7777-777777777777",
  "anuncioId": "11111111-1111-1111-1111-111111111111",
  "denuncianteId": "33333333-3333-3333-3333-333333333333",
  "motivo": "Fraude",
  "descricao": "Anúncio duplicado suspeito",
  "status": "RESOLVIDA",
  "resolvidaEm": "2026-11-22T16:10:00Z",
  "criadoEm": "2026-11-22T16:00:00Z"
}
```

A flag `moderado` está no response, mas não remove anúncio da busca; para isso use `DELETE /admin/anuncios/{id}` ou `removerAnuncio=true` na resolução.

---

## Endpoints fora de `/api/v1`

| Método | Rota | Auth | Exemplo | Resposta |
|---|---|---|---|---|
| GET | `/healthz` | Pública | `curl https://seu-host/healthz` | `200` `{"status":"ok"}` (não consulta dependências) |
| GET | `/actuator/health` | Pública | `curl https://seu-host/actuator/health` | `200` health completo |
| GET | `/actuator/health/liveness` | Pública | `curl https://seu-host/actuator/health/liveness` | `200` liveness |
| GET | `/actuator/info` | Pública | `curl https://seu-host/actuator/info` | `200` metadados da app |
| GET | `/actuator/metrics` | Pública | `curl https://seu-host/actuator/metrics` | `200` nomes de métricas |
| GET | `/v3/api-docs` | Pública | `curl https://seu-host/v3/api-docs` | `200` OpenAPI JSON canônico |
| GET | `/openapi.json` | Pública | `curl https://seu-host/openapi.json` | `200` alias do documento OpenAPI |
| GET | `/swagger-ui.html` | Pública | abrir no navegador | UI do Swagger |
| GET | `/uploads/**` | Pública | `curl https://seu-host/uploads/anuncios/foto.jpg` | arquivo de imagem local |

Somente `health`, `info` e `metrics` são expostos pelo Actuator. O CSP permite `self`, `script-src`, `style-src` e `connect-src` necessários ao Swagger UI, mantendo `frame-ancestors 'none'`.
