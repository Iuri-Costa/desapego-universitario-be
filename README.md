# Desapego Universitário — Backend
- **URL Base da API:** [https://desapego-universitario-be.onrender.com](https://desapego-universitario-be.onrender.com)
- **Endpoint de Anúncios:** `/anuncios`

Marketplace de economia circular para estudantes universitários, permitindo que anunciem itens (livros, eletrônicos, móveis, etc.) para venda ou doação dentro do campus. Feito como desafio técnico do processo seletivo do Laboratório Vortex (Unifor), etapa VORTEX 2026.2.

Esse repositório é o backend, uma API REST em Spring Boot. O frontend fica em repositório separado: `desapego-universitario-fe`.

## Tecnologias

- Java + Spring Boot
- Spring Data JPA
- PostgreSQL (hospedado no Render)
- Spring Security (usado apenas para o `BCryptPasswordEncoder`, hash de senha)
- Maven

## Como rodar

Pré requisitos: Java 21+, Maven (ou usa o wrapper `./mvnw`, que já vem no repositório), e uma instância PostgreSQL acessível (local ou na nuvem).

1. Cria um arquivo `.env` na raiz do projeto com:
   JDBC_URL=jdbc:postgresql://<host>:<porta>/<nome_do_banco>
   DB_USERNAME=<usuario>
   DB_PASSWORD=<senha>
2. Roda a aplicação:

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. As tabelas são criadas/atualizadas automaticamente pelo Hibernate (`ddl-auto=update`), não precisa rodar migração manual.

## Endpoints

### Anúncios

| Método | Rota | Descrição |
|---|---|---|
| POST | `/anuncios` | Cria um anúncio |
| GET | `/anuncios` | Lista todos os anúncios |
| GET | `/anuncios/categoria/{categoria}` | Lista anúncios de uma categoria (`LIVROS`, `ELETRONICOS`, `ENGENHARIA`, `COMPUTACAO`, `MOVEIS`, `VESTUARIO`, `PAPELARIA`, `OUTROS`) |
| DELETE | `/anuncios/{id}` | Deleta um anúncio pelo id |

### Usuários

| Método | Rota | Descrição |
|---|---|---|
| POST | `/usuarios/cadastro` | Cria uma conta (nome, email, senha) |
| POST | `/usuarios/login` | Autentica um usuário (email, senha) |

Todas as rotas retornam e recebem JSON. Senhas são armazenadas com hash (BCrypt), nunca em texto puro.

<p align=center>Diário de Bordo da IA</p>

### Ferramentas Utilizadas
- Claude (Anthropic) — usado ao longo de todo o desenvolvimento do backend.

### Estratégia de Engenharia de Prompts

Exemplo 1 — decisão de arquitetura (DTO vs expor entidade direto):
Ao planejar cadastro/login, perguntei se deveria usar DTOs. Discuti com a IA por que a entidade `Anuncio` podia ser exposta direto no Controller sem problema, mas `Usuario` (por ter campo de senha) exigia DTOs separados de entrada e saída — decisão de design, não só sintaxe.

Exemplo 2 — organização do histórico de commits:
Percebi que tinha acumulado várias mudanças (entidade, service, repository, DTOs, controller) sem organizar em commits — inclusive com arquivos ficando "staged" com versão desatualizada em relação ao que eu tinha escrito depois. Pedi ajuda pra revisar isso com `git diff`, usar `git reset` pra desfazer staging incorreto, e separar os commits por grupo lógico de arquivos.

Exemplo 3 — avaliação de escopo (autenticação JWT):
Não conhecia JWT antes desse desafio. Pedi pra IA explicar o conceito e implementei uma classe `JwtUtil` funcional (geração e validação de token) com revisão da IA. Depois, avaliando o tempo restante e o que ainda faltava (frontend/PWA obrigatório, ainda não iniciado), decidi remover o JWT do escopo do projeto.

### Reflexão Crítica
A implementação de JWT chegou a funcionar, mas percebi que mantê-la comprometeria o tempo necessário pra entregar o restante do desafio dentro do prazo. Removi essa parte do projeto, mantendo cadastro e login com validação de email/senha (hash via BCrypt), sem geração de token. Foi um momento de precisar avaliar criticamente o que a IA ajudou a construir — só porque algo funciona não significa que cabe no escopo e no tempo disponíveis.
