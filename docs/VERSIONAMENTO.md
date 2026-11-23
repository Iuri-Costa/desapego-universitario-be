# Política de versionamento e depreciação da API

## Versionamento

- A API é versionada **na URL**: `/api/v1/...`. Não usamos versionamento por header nem por query param.
- `v1` é a versão pública atual. Uma nova versão (`/api/v2`) só é criada quando uma mudança **incompatível** for necessária.
- Mudanças compatíveis dentro de `v1` são incrementos de versão da documentação, sem quebrar clientes:
  - adição de endpoint;
  - adição de campo **opcional** na resposta (clientes Retrofit/Ktor devem ignorar campos desconhecidos);
  - adição de parâmetro de query **opcional** com default preservando o comportamento anterior;
  - adição de novo valor de enum (clientes devem tratar valores desconhecidos com fallback, nunca quebrar);
  - correção de bug que não altera o contrato publicado.
- Mudanças incompatíveis exigem nova versão de URL:
  - remoção/renomeação de endpoint, campo ou parâmetro;
  - mudança de tipo de campo, de formato de data ou de semântica de status;
  - remoção de valor de enum;
  - mudança no formato de paginação ou de erro.

O contrato publicado é o `openapi.json` (`/openapi.json`), usado para gerar os clientes Kotlin.
Toda mudança de contrato deve atualizar esse arquivo e ser revisada antes do merge.

## Compatibilidade de contrato

| Item | Regra em `v1` |
| --- | --- |
| Datas | ISO 8601 em UTC, sempre com `Z` (ex.: `2026-11-22T16:00:00Z`) |
| IDs | UUID no formato canonical (8-4-4-4-12) |
| Enums | Nomes em `UPPER_SNAKE_CASE`, estáveis; entrada aceita qualquer caixa |
| Paginação | `page` (base 0), `size` (1–100, default 20) e resposta `PaginadoDTO<T>` com `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last` |
| Erros | `ErroResponse` com `timestamp`, `status`, `codigo`, `mensagem`, `caminho`, `erros` |
| Autenticação | `Authorization: Bearer <accessToken>` |

Clientes **não devem** depender da ordem das chaves JSON, de campos não documentados nem de valores de enum não listados no OpenAPI.

## Depreciação

Quando um endpoint precisar ser removido, ele passa pelo ciclo:

1. **Anúncio**: marcar a operação como `deprecated` no OpenAPI e documentar a substituta neste arquivo.
2. **Aviso ao cliente**: responder com os headers `Deprecation: true` e `Sunset: <RFC 7231 HTTP-date>`.
3. **Prazo**: no mínimo **90 dias** entre o aviso e o desligamento, salvo risco de segurança (ex.: vazamento de credenciais), em que a remoção pode ser imediata.
4. **Desligamento**: o endpoint antigo retorna `410 Gone` com `ErroResponse`; a remoção física do código só ocorre na major seguinte.
5. **Migração**: todos os clientes internos devem estar na substituta antes do `Sunset`.

Exemplo de header:

```http
Deprecation: true
Sunset: Wed, 31 Mar 2027 00:00:00 GMT
Link: </api/v2/anuncios>; rel="successor-version"
```

### Depreciações ativas

| Rota legada | Substituta | Aviso desde | Sunset |
| --- | --- | --- | --- |
| `POST /api/v1/usuarios/login` | `POST /api/v1/auth/login` | 2026-11-23 | 2027-03-31 (`Deprecation: true` e `Sunset` em toda resposta 200) |

Ambas emitem o mesmo par access/refresh token com rotação; a substituta é idêntica em corpo e códigos de erro.

## Política de suporte

- `v1`: suporte integral; correções de bug e mudanças compatíveis.
- `v0`/rotas sem prefixo: não suportadas — retornam `404`.
- Versões antigas recebem correções de segurança enquanto coexistirem com a versão seguinte; após o `Sunset`, apenas a versão atual é suportada.

## Checklist para mudanças de contrato

- [ ] `openapi.json` atualizado (exemplos, códigos de resposta e security scheme)
- [ ] `docs/API.md` atualizado quando aplicável
- [ ] Migração Flyway aditiva, sem perda de dados, quando o schema mudar
- [ ] Testes de integração cobrindo o novo contrato
- [ ] `./mvnw test` verde
- [ ] Aviso de depreciação com `Sunset` quando houver substituição
