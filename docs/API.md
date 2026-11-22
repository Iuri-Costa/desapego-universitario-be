# API REST — guia para clientes (Retrofit / Ktor)

Base URL: `http://localhost:8080` (em produção, use a URL do serviço).
Contrato completo: `GET /openapi.json` · UI: `GET /swagger-ui.html`.
Política de versão e depreciação: [`docs/VERSIONAMENTO.md`](VERSIONAMENTO.md).

Todas as rotas usam o prefixo `/api/v1`. Rotas sem versão retornam `404`.
Respostas e requisições são JSON (`application/json`), exceto upload de imagens (`multipart/form-data`).

> Ao gerar o cliente Kotlin a partir do `openapi.json`, configure o gerador para manter nomes
> de enums em `UPPER_SNAKE_CASE` e para **ignorar campos desconhecidos** (`ignoreUnknownKeys = true`).
> Isso preserva compatibilidade quando a API adiciona campos novos.

---

## 1. Convenções do contrato

| Item | Formato |
| --- | --- |
| Identificadores | UUID (string), ex.: `9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d` |
| Datas de domínio | ISO 8601 em UTC, terminando em `Z`, ex.: `2026-11-22T16:00:00Z` |
| Enums | `UPPER_SNAKE_CASE` na saída; a API aceita qualquer caixa na entrada |
| Dinheiro | Número decimal (JSON `number`), ex.: `80.00` |
| Erros | `ErroResponse` (ver §5) |

---

## 2. Autenticação

### 2.1 Cadastro e verificação

```
POST /api/v1/usuarios/cadastro
{ "nome": "Maria Almeida", "email": "maria@universidade.edu.br", "senha": "Senha@12345" }
```

- E-mail institucional obrigatório (domínios configuráveis: `edu.br`, `edu`, `ac.br` por padrão).
- Senha: 8–72 caracteres, com maiúscula, minúscula, dígito e símbolo.
- O cadastro envia um token de verificação por e-mail. Confirmar com:

```
POST /api/v1/usuarios/verificar-email
{ "token": "<token recebido por e-mail>" }
```

Sem e-mail verificado, o login falha com `401 CREDENCIAIS_INVALIDAS`.
`POST /api/v1/usuarios/reenviar-verificacao` reenvia o token.

### 2.2 Login

Endpoint recomendado: `POST /api/v1/auth/login` (o antigo `POST /api/v1/usuarios/login` continua compatível).

```
POST /api/v1/auth/login
{ "email": "maria@universidade.edu.br", "senha": "Senha@12345" }

200 OK
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
  "refreshToken": "eyJhbGciOiJIUzM4NCJ9...",
  "tipo": "Bearer",
  "expiresIn": 900
}
```

- `accessToken`: JWT HS256, válido por **15 minutos** (`expiresIn` em segundos).
- `refreshToken`: JWT opaco para o cliente, válido por **7 dias**, de **uso único**.
- Credenciais inválidas, usuário inexistente e e-mail não verificado retornam a **mesma** mensagem genérica.
- Rate limit por IP: 5 logins/minuto (configurável). Ao exceder: `429 LIMITE_EXCEDIDO` com header `Retry-After`.

### 2.3 Usando o access token

Envie em **toda** rota autenticada:

```http
Authorization: Bearer <accessToken>
```

Exceções públicas: cadastro/login/verificação/recuperação, `GET /api/v1/anuncios`,
`GET /uploads/**`, `/openapi.json` e `/swagger-ui.html`.

### 2.4 Refresh token (rotação)

```
POST /api/v1/auth/refresh
{ "refreshToken": "<refresh atual>" }

200 OK
{ "accessToken": "<novo access>", "refreshToken": "<novo refresh>", "tipo": "Bearer", "expiresIn": 900 }
```

Regras importantes para o cliente:

1. **Sempre substitua** o refresh guardado pelo novo retornado.
2. O refresh antigo é revogado na rotação. Se for reutilizado, a API **revoga toda a família**
   de tokens daquele usuário (todas as sessões precisam fazer login novamente).
3. Portanto, **nunca dispare duas renovações concorrentes**: use um lock/mutex e descarte
   requisições enfileiradas que receberam `401` enquanto a renovação acontecia.
4. `POST /api/v1/auth/logout` (autenticado) revoga todos os refresh tokens do usuário.
5. Armazene tokens em local seguro (Keychain/Keystore), nunca em logs, analytics ou crash reports.
6. Se o refresh falhar (`401 TOKEN_INVALIDO`), limpe a sessão e volte ao login.

### 2.5 Retrofit

```kotlin
interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<Unit>
}

val auth = Retrofit.Builder()
    .baseUrl("http://localhost:8080/")
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .client(okHttpComInterceptorDeBearer())
    .build()
    .create(AuthApi::class.java)
```

Use um `Authenticator` do OkHttp com um `Mutex` (ou `Semaphore(1)`) para a renovação.
Em paralelo, um interceptor de `401` deve redescartar apenas a requisição que recebeu o
primeiro `401`; as demais devem repetir com o novo token já atualizado.

### 2.6 Ktor

```kotlin
val client = HttpClient(CIO) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(Auth) {
        bearer {
            loadTokens { BearerTokens(tokenStore.access, tokenStore.refresh) }
            refreshTokens {
                val response = client.post("http://localhost:8080/api/v1/auth/refresh") {
                    setBody(RefreshRequest(tokenStore.refresh))
                    markAsRefreshTokenRequest()
                }
                val tokens = response.body<TokenResponse>()
                tokenStore.save(tokens.accessToken, tokens.refreshToken)
                BearerTokens(tokens.accessToken, tokens.refreshToken)
            }
        }
    }
}
```

---

## 3. Paginação, filtros e ordenação

Endpoints paginados (busca de anúncios, favoritos, histórico, notificações, denúncias) usam:

- Query params: `page` (base **0**, default `0`), `size` (1–100, default `20`), `sort` quando documentado.
- Resposta sempre com o mesmo envelope:

```json
{
  "content": [ { "id": "...", "titulo": "Calculadora" } ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "first": true,
  "last": false
}
```

`page < 0`, `size < 1`, `size > 100` retornam `400 PARAMETRO_INVALIDO`.

### Busca de anúncios

```
GET /api/v1/anuncios?q=calculadora&categoria=LIVROS&tipo=VENDA&condicao=SEMINOVO
    &precoMin=10&precoMax=200&campus=PRINCIPAL&status=ATIVO
    &page=0&size=20&sort=preco,asc;criadoEm,desc
```

- Filtros são combináveis e opcionais. Removidos (soft delete) nunca aparecem.
- `sort` aceita apenas: `titulo`, `preco`, `categoria`, `condicao`, `tipo`, `status`,
  `campus`, `criadoEm`, `atualizadoEm`; direção `asc` ou `desc`.
  Múltiplos critérios são separados por `;`. Padrão: `criadoEm,desc`.
- Exemplo Retrofit:

```kotlin
@GET("api/v1/anuncios")
suspend fun buscar(
    @Query("q") q: String? = null,
    @Query("categoria") categoria: String? = null,
    @Query("page") page: Int = 0,
    @Query("size") size: Int = 20,
    @Query("sort") sort: String = "criadoEm,desc"
): PaginadoDTO<AnuncioResponse>
```

---

## 4. Upload de imagens

```
POST /api/v1/anuncios/{id}/imagens
Authorization: Bearer <accessToken>
Content-Type: multipart/form-data

imagens: <arquivo 1>
imagens: <arquivo 2>
```

- O nome do campo é **`imagens`** (repetível); envie de 1 a 5 arquivos por chamada.
- Tipos aceitos: `image/jpeg`, `image/png`, `image/webp`. O **magic bytes** é validado,
  então renomear um arquivo não funciona.
- Tamanho máximo por arquivo: **2 MB**. Limites de infra: 10 MB por arquivo / 25 MB por requisição.
- Cada anúncio aceita no máximo **5 imagens no total** (soma de todas as chamadas).
- Somente o **dono** do anúncio pode enviar, listar ou remover imagens.
- Resposta: `200 OK` com `[{ "id": "<uuid>", "url": "https://.../anuncios/<uuid>.jpg" }]`.
- Em desenvolvimento sem `S3_BUCKET`, as URLs são locais (`/uploads/...`); em produção, URLs do S3.
- Erros comuns: `400 IMAGEM_INVALIDA`, `400 REQUISICAO_INVALIDA` (campo ausente),
  `403 ACESSO_NEGADO`, `404 NAO_ENCONTRADO`.

Retrofit:

```kotlin
@Multipart
@POST("api/v1/anuncios/{id}/imagens")
suspend fun enviarImagens(
    @Path("id") id: String,
    @Part imagens: List<MultipartBody.Part>
): List<ImagemResponse>
```

Ktor:

```kotlin
val body = MultiPartFormDataContent(formData {
    files.forEach { file ->
        append("imagens", file.name, ContentType.Application.OctetStream, file.readBytes())
    }
})
client.post("http://localhost:8080/api/v1/anuncios/$id/imagens") { setBody(body) }
```

Para `ContentType`, use o MIME real da imagem (`image/jpeg`, `image/png`, `image/webp`).

---

## 5. Erros

Formato único para todas as respostas de erro:

```json
{
  "timestamp": "2026-11-22T16:00:00Z",
  "status": 400,
  "codigo": "VALIDACAO",
  "mensagem": "Erro de validação",
  "caminho": "/api/v1/anuncios",
  "erros": [
    { "campo": "titulo", "mensagem": "O título é obrigatório" }
  ]
}
```

`timestamp` é ISO 8601 com offset. Trate pelo **`codigo`**, nunca pela mensagem
(textos podem mudar). `erros` vem preenchido em erros de validação de corpo.

| HTTP | `codigo` | Quando |
| --- | --- | --- |
| 400 | `VALIDACAO` | Campo inválido no corpo (veja `erros`) |
| 400 | `REQUISICAO_INVALIDA` | JSON malformado ou parte `imagens` ausente |
| 400 | `PARAMETRO_INVALIDO` | Query/path inválido (ex.: `page=-1`, `sort` fora da whitelist) |
| 400 | `PARAMETRO_OBRIGATORIO` | Query param obrigatório ausente |
| 400 | `IMAGEM_INVALIDA` | Tipo/tamanho/conteúdo de imagem inválido |
| 400 | `DOMINIO_INVALIDO` | E-mail não institucional |
| 401 | `CREDENCIAIS_INVALIDAS` | Login inválido ou e-mail não verificado |
| 401 | `TOKEN_INVALIDO` | Refresh inválido, expirado ou reutilizado |
| 401 | `NAO_AUTENTICADO` | Falta Bearer token |
| 403 | `ACESSO_NEGADO` | Usuário autenticado sem permissão (ex.: não dono, não ADMIN) |
| 404 | `NAO_ENCONTRADO` | Recurso/rota inexistente |
| 405 | `METODO_NAO_SUPORTADO` | Verbo HTTP inválido para a rota |
| 409 | `CONFLITO` | Estado inválido (ex.: favorito duplicado, denúncia duplicada) |
| 409 | `CONFLITO_DE_DADOS` | Violação de integridade no banco |
| 429 | `LIMITE_EXCEDIDO` | Rate limit; ver header `Retry-After` |
| 500 | `ERRO_INTERNO` | Erro inesperado (sem detalhes sensíveis) |

Exemplo de tratamento Retrofit:

```kotlin
sealed interface ApiError {
    data class Http(val codigo: String, val mensagem: String, val status: Int) : ApiError
    data object Rede : ApiError
}

suspend fun <T> execute(chamada: suspend () -> T): T = try {
    chamada()
} catch (e: HttpException) {
    val erro = e.response()?.errorBody()?.string()
        ?.let { Json.decodeFromString<ErroResponse>(it) }
    throw ApiException(ApiError.Http(erro?.codigo ?: "ERRO_INTERNO",
        erro?.mensagem ?: "Erro desconhecido", e.code()))
} catch (e: IOException) {
    throw ApiException(ApiError.Rede)
}
```

---

## 6. Outros endpoints

- Anúncios: `POST/PATCH/PUT/DELETE /api/v1/anuncios/{id}`, `GET /api/v1/anuncios/{id}`,
  `GET /api/v1/anuncios/categoria/{categoria}`.
- Favoritos: `POST/DELETE /api/v1/anuncios/{id}/favoritos`, `GET /api/v1/usuarios/me/favoritos`.
- Perfil: `GET/PATCH /api/v1/usuarios/me`, `GET /api/v1/usuarios/me/anuncios`.
- Mensagens: `POST/GET /api/v1/anuncios/{id}/mensagens`, `PATCH /api/v1/mensagens/{id}/lida`.
- Negociação: `POST/DELETE /api/v1/anuncios/{id}/reserva`, `POST /api/v1/anuncios/{id}/conclusao`,
  `POST /api/v1/anuncios/{id}/avaliacoes`, `GET /api/v1/usuarios/{id}/avaliacoes`.
- Denúncias: `POST /api/v1/anuncios/{id}/denuncias`.
- Moderação (ADMIN): `/api/v1/admin/**`.
- Notificações: `GET /api/v1/notificacoes`, `GET /nao-lidas`, `PATCH /{id}/lida`,
  `POST/DELETE /api/v1/notificacoes/dispositivo` (token FCM).

Consulte o `openapi.json` para payloads, exemplos e códigos de resposta de cada operação.
