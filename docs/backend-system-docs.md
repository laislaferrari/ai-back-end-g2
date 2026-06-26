# Back-end System Docs — MindJournal AI

## 1. Visão geral do back-end

Sistema de diário pessoal inteligente com API REST simulando respostas de IA, gerenciamento de sessões de conversa, histórico de mensagens e upload de arquivos TXT/PDF. Construído com Spring Boot 3, Java 17 e Maven, exposto na porta 8080.

## 2. Objetivos da API

- Servir como fonte da verdade para o front-end React
- Gerenciar criação e recuperação de sessões de diário
- Receber mensagens do usuário e retornar respostas textuais simuladas
- Persistir e recuperar histórico de mensagens por sessão
- Receber upload de arquivos TXT e PDF com validação
- Registrar metadados dos arquivos anexados
- Expor endpoint de monitoramento da saúde da API
- Ser preparada para futura substituição das respostas simuladas por integração real com IA

## 3. Árvore de diretórios proposta

```
src/
├── main/
│   ├── java/com/mindjournal/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── dto/
│   │   ├── config/
│   │   └── exception/
│   └── resources/
│       └── application.yml
└── test/
    └── java/com/mindjournal/
```

## 4. Responsabilidade de cada camada

| Camada | Responsabilidade |
|--------|-----------------|
| `controller` | Fronteira HTTP — recebe requisições, delega para services, converte e retorna respostas. Sem regras de negócio. |
| `service` | Lógica da aplicação e orquestração do domínio. Independe de conceitos HTTP. Contém todas as validações de negócio. |
| `repository` | Acesso exclusivo a dados (JPA/Hibernate). Apenas queries e persistência. |
| `model` | Entidades de domínio — Session, Message, Attachment. Podem utilizar anotações JPA, mas não devem conter responsabilidades HTTP ou regras de orquestração da aplicação. |
| `dto` | Contratos de entrada e saída da API. Imutáveis, sem lógica de negócio. |
| `config` | Configurações da aplicação (CORS, upload size, IA strategy). |
| `exception` | Tratamento global de erros com `@ControllerAdvice` e `ProblemDetail` (RFC 9457). |

## 5. Modelo conceitual das entidades

### Session

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | Long (PK) | Identificador único |
| title | String | Título opcional da sessão |
| createdAt | Instant (ISO 8601 UTC) | Data de criação |
| updatedAt | Instant (ISO 8601 UTC) | Data da última atividade |

### Message

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | Long (PK) | Identificador único |
| session | Session (FK) | Sessão à qual pertence |
| sender | Enum (USER / ASSISTANT) | Remetente da mensagem |
| content | String (TEXT) | Conteúdo textual |
| timestamp | Instant (ISO 8601 UTC) | Data e hora do envio |

### Attachment

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | Long (PK) | Identificador único |
| session | Session (FK) | Sessão à qual pertence |
| filename | String | Nome original do arquivo |
| type | Enum (TXT / PDF) | Tipo do arquivo |
| size | Long | Tamanho em bytes |
| filePath | String | Caminho de armazenamento |
| uploadDate | Instant (ISO 8601 UTC) | Data do upload |

## 6. Relacionamentos entre as entidades

```
Session ──1:N──> Message
Session ──1:N──> Attachment
```

- Uma sessão pode conter zero ou muitas mensagens.
- Uma sessão pode conter zero ou muitos anexos.
- Mensagens e anexos pertencem a uma única sessão.

## 7. DTOs previstos

| DTO | Uso |
|-----|-----|
| `ChatRequest` | Payload de entrada do POST /api/chat — `sessionId`, `content` |
| `ChatResponse` | Payload de saída do POST /api/chat — `userMessage` (MessageDTO), `assistantMessage` (MessageDTO) |
| `SessionDTO` | Representação de uma sessão — `id`, `title`, `createdAt`, `updatedAt` |
| `CreateSessionRequest` | Payload de criação de sessão — `title` (opcional) |
| `MessageDTO` | Representação de uma mensagem — `id`, `sessionId`, `sender`, `content`, `timestamp` |
| `AttachmentDTO` | Representação de um anexo — `id`, `sessionId`, `filename`, `type`, `size`, `uploadDate` |
| `HealthDTO` | Resposta do health check — `status`, `timestamp` |

## 8. Controllers previstos

| Controller | Endpoints |
|------------|-----------|
| `HealthController` | GET /api/health |
| `SessionController` | POST /api/sessions, GET /api/sessions, GET /api/sessions/{id} |
| `ChatController` | POST /api/chat |
| `MessageController` | GET /api/sessions/{id}/messages |
| `AttachmentController` | POST /api/upload, GET /api/sessions/{id}/attachments (opcional Etapa 1) |

O `AttachmentController` recebe o `MultipartFile` e o converte para um objeto de entrada independente de HTTP (nome, tipo MIME, tamanho, conteúdo do arquivo e `sessionId`) antes de chamar `AttachmentService`.

## 9. Services previstos

| Service | Responsabilidade |
|---------|-----------------|
| `SessionService` | Criar, listar e buscar sessões. Listagem ordenada por `updatedAt` decrescente. |
| `MessageService` | Persistir mensagens e recuperar histórico por sessão ordenado por `timestamp` crescente. |
| `ChatService` | Orquestrar recebimento de mensagem, validar existência da sessão, persistir mensagens e gerar resposta simulada. |
| `AttachmentService` | Processar upload, validar tipo/extensão/tamanho, validar existência da sessão, salvar arquivo e metadados. Não recebe `MultipartFile` diretamente — recebe nome, tipo MIME, tamanho, conteúdo do arquivo e `sessionId`. |
| `AiResponseGenerator` | Interface para geração de respostas (mock agora, IA real no futuro). |

## 10. Repositories previstos

| Repository | Entidade |
|------------|----------|
| `SessionRepository` | Session |
| `MessageRepository` | Message |
| `AttachmentRepository` | Attachment |

## 11. Tabela dos contratos da API REST

| Método | Rota | Finalidade | Parâmetros | Payload de entrada | Payload de saída | Códigos |
|--------|------|------------|------------|-------------------|-------------------|---------|
| GET | `/api/health` | Verificar saúde da API | — | — | `{ "status": "UP", "timestamp": "2026-06-26T10:00:00Z" }` | 200 |
| POST | `/api/sessions` | Criar nova sessão | — | `{ "title": "string" }` | `SessionDTO` | 201 |
| GET | `/api/sessions` | Listar todas as sessões | — | — | `[SessionDTO]` (ordenado por `updatedAt` desc) | 200 |
| GET | `/api/sessions/{id}` | Buscar sessão por ID | path: id | — | `SessionDTO` | 200, 404 |
| POST | `/api/chat` | Enviar mensagem e receber resposta | — | `{ "sessionId": 1, "content": "..." }` | `{ "userMessage": MessageDTO, "assistantMessage": MessageDTO }` | 201, 404 |
| GET | `/api/sessions/{id}/messages` | Recuperar histórico de mensagens | path: id | — | `[MessageDTO]` (ordenado por `timestamp` asc) | 200, 404 |
| POST | `/api/upload` | Fazer upload de arquivo | multipart: file, sessionId | `file` (TXT/PDF), `sessionId` (obrigatório) | `AttachmentDTO` | 201, 400, 404 |
| GET | `/api/sessions/{id}/attachments` | Listar anexos de uma sessão (opcional Etapa 1) | path: id | — | `[AttachmentDTO]` | 200, 404 |

## 12. Fluxo de criação de sessão

1. Usuário clica em "Nova sessão" no front-end
2. Front-end envia `POST /api/sessions` com `title` opcional
3. `SessionController` recebe a requisição, converte para `CreateSessionRequest` e delega para `SessionService`
4. `SessionService` cria entidade `Session` com `createdAt` e `updatedAt`
5. `SessionRepository` persiste a entidade no banco
6. `SessionController` retorna `SessionDTO` com status 201
7. Front-end atualiza a lista de sessões

## 13. Fluxo de envio de mensagem

1. Usuário digita mensagem e pressiona Enter no front-end
2. Front-end envia `POST /api/chat` com `{ "sessionId": 1, "content": "..." }`
3. `ChatController` recebe a requisição, converte para `ChatRequest` e delega para `ChatService`
4. `ChatService` valida existência da sessão via `SessionService`
5. `ChatService` persiste a mensagem do usuário via `MessageService`
6. `ChatService` invoca `AiResponseGenerator` para obter resposta simulada
7. Resposta simulada é persistida como mensagem do assistente via `MessageService`
8. `ChatController` retorna `{ "userMessage": MessageDTO, "assistantMessage": MessageDTO }` com status 201
9. Front-end exibe ambas as mensagens no chat

## 14. Fluxo de recuperação do histórico

1. Usuário seleciona uma sessão no front-end
2. Front-end envia `GET /api/sessions/{id}/messages`
3. `MessageController` recebe a requisição e delega para `MessageService`
4. `MessageService` valida existência da sessão via `SessionService`
5. `MessageService` busca mensagens ordenadas por `timestamp` ascendente
6. `MessageController` retorna lista de `MessageDTO` com status 200
7. Front-end renderiza as mensagens no chat

## 15. Fluxo de upload de documentos

1. Usuário arrasta arquivo ou clica para selecionar no front-end
2. Front-end envia `POST /api/upload` com `multipart/form-data` contendo `file` e `sessionId` (obrigatório)
3. `AttachmentController` recebe o `MultipartFile` e extrai nome, tipo MIME, tamanho, conteúdo e `sessionId`, convertendo para um objeto de entrada sem dependência HTTP; delega para `AttachmentService`
4. `AttachmentService` recebe nome, tipo MIME, tamanho, conteúdo do arquivo e `sessionId`; valida existência da sessão via `SessionService`
5. `AttachmentService` valida extensão (.txt, .pdf), tipo MIME e tamanho máximo (10 MB)
6. Arquivo é salvo no sistema de arquivos (diretório configurável)
7. `AttachmentService` persiste metadados via `AttachmentRepository`
8. `AttachmentController` retorna `AttachmentDTO` com status 201
9. Em caso de arquivo inválido ou sessão inexistente, retorna 400 ou 404 com `ProblemDetail`
10. Front-end exibe progresso durante o upload e confirmação ao final

## 16. Estratégia de persistência relacional

- H2 em modo arquivo na primeira etapa (URL `jdbc:h2:file:./data/mindjournal`), garantindo persistência após reinicialização
- PostgreSQL reservado como possibilidade futura de migração
- JPA / Hibernate com `spring-boot-starter-data-jpa`
- Esquema gerado automaticamente pelas entidades (`ddl-auto: update`)
- `schema.sql` não será utilizado — o schema é gerenciado exclusivamente pelo JPA
- Relacionamentos:
  - `Session` → `Message`: `@OneToMany(mappedBy = "session", cascade = ALL)`
  - `Session` → `Attachment`: `@OneToMany(mappedBy = "session", cascade = ALL)`
  - `Message` → `Session`: `@ManyToOne(fetch = LAZY)`
  - `Attachment` → `Session`: `@ManyToOne(fetch = LAZY)`
- Índices em `session_id` nas tabelas `messages` e `attachments` para consultas por sessão

## 17. Validações e tratamento de erros

- **Arquivos:** apenas `.txt` e `.pdf`; tamanho máximo: 10 MB; um arquivo por requisição
- **Mensagens:** conteúdo não pode ser vazio ou apenas espaços
- **Sessão:** validação de existência em operações que referenciam `sessionId` — realizada nos Services, nunca nos Controllers
- **Tratamento global:** `@ControllerAdvice` com `ProblemDetail` (RFC 9457)
- **Campos do `ProblemDetail` utilizados pelo front-end:** `status` (int), `title` (string), `detail` (string)
- **Códigos de erro:**
  - 400 — Bad Request (arquivo inválido, conteúdo vazio, extensão não permitida)
  - 404 — Not Found (sessão inexistente)
  - 413 — Payload Too Large (arquivo excede 10 MB)
  - 500 — Internal Server Error (falhas não esperadas)

## 18. Restrições arquiteturais

- Controllers atuam somente como fronteira HTTP — recebem, convertem e delegam; sem regras de negócio
- Validação de existência de sessão ocorre nos Services, nunca nos Controllers
- Validação de tipo, extensão e tamanho do arquivo ocorre no `AttachmentService`
- Services concentram lógica e orquestração — sem dependências de `HttpServletRequest`, `MultipartFile`, etc.
- Repositories tratam exclusivamente acesso a dados — sem lógica de negócio
- DTOs representam contratos de entrada/saída — imutáveis, sem lógica
- Camada de domínio não conhece detalhes concretos de banco ou transporte HTTP
- `GET /api/health` é obrigatório
- Upload aceita apenas `.txt` e `.pdf`, um arquivo por requisição, com `sessionId` obrigatório
- Arquitetura preparada para substituição futura da resposta simulada por IA real sem alterar o núcleo do domínio
- Datas e horários em formato ISO 8601 UTC (`Instant`)
- Sessões listadas por `updatedAt` decrescente
- Mensagens listadas por `timestamp` crescente

## 19. Critérios de aceite da primeira etapa

- [ ] `GET /api/health` retorna 200 com status UP
- [ ] `POST /api/sessions` cria sessão e retorna 201 com dados da sessão
- [ ] `GET /api/sessions` retorna lista de sessões ordenada por `updatedAt` desc (pode ser vazia)
- [ ] `GET /api/sessions/{id}` retorna sessão específica ou 404
- [ ] `POST /api/chat` com `sessionId` válido persiste ambas as mensagens e retorna 201 com `userMessage` e `assistantMessage`
- [ ] `POST /api/chat` com `sessionId` inválido retorna 404
- [ ] `GET /api/sessions/{id}/messages` retorna histórico ordenado por `timestamp` asc ou 404
- [ ] `POST /api/upload` com `sessionId` válido e `.txt`/`.pdf` válido salva arquivo e metadados, retorna 201
- [ ] `POST /api/upload` sem `sessionId` retorna 400
- [ ] `POST /api/upload` com `sessionId` inválido retorna 404
- [ ] `POST /api/upload` com tipo inválido retorna 400 com `ProblemDetail`
- [ ] `POST /api/upload` com arquivo > 10 MB retorna 413 com `ProblemDetail`
- [ ] `GET /api/sessions/{id}/attachments` é opcional e não bloqueia os requisitos mínimos
- [ ] Nenhum controller contém regra de negócio
- [ ] Nenhum service depende de objetos HTTP
- [ ] DTOs são imutáveis
- [ ] Erros utilizam `ProblemDetail` com campos `status`, `title` e `detail`

## 20. Estratégia para futura integração com IA

- Definir interface `AiResponseGenerator` no domínio:

```
interface AiResponseGenerator {
    String generateResponse(Long sessionId, String userMessage);
}
```

- Implementação concreta `MockAiResponseGenerator` para primeira etapa — retorna respostas pré-definidas baseadas em palavras-chave
- Futura implementação `OpenAiResponseGenerator` ou `ClaudeResponseGenerator` injetada via Spring `@Profile` ou `@ConditionalOnProperty`
- A troca entre mock e IA real ocorre por configuração (`application.yml`), sem alterar `ChatService` ou camadas superiores
- Preparar `MessageDTO` para suportar future streaming com WebSocket ou SSE

---

## Decisões aprovadas pela equipe

1. **Stack:** Java 17, Spring Boot 3 e Maven no back-end.
2. **Stack front-end:** React, TypeScript, Vite e Axios.
3. **Persistência:** H2 em modo arquivo na primeira etapa (`jdbc:h2:file:./data/mindjournal`), garantindo dados após reinício.
4. **PostgreSQL:** Reservado como possibilidade futura, não implementado agora.
5. **JPA:** `ddl-auto: update` gera o schema automaticamente; `schema.sql` não será utilizado.
6. **Upload:** `sessionId` é obrigatório. O multipart contém os campos `file` e `sessionId`.
7. **Validação de sessão:** ocorre nos Services (via `SessionService`), nunca nos Controllers.
8. **Validação de arquivo:** tipo, extensão e tamanho validados no `AttachmentService`.
9. **Controllers:** apenas recebem, convertem e delegam — sem regras de negócio.
10. **Contrato `POST /api/chat`:** retorna `userMessage` e `assistantMessage`, ambos `MessageDTO`.
11. **Datas:** ISO 8601 UTC (`Instant`) em todas as respostas e entidades.
12. **Ordenação de sessões:** por `updatedAt` decrescente.
13. **Ordenação de mensagens:** por `timestamp` crescente.
14. **Limite de upload:** 10 MB, um arquivo por requisição.
15. **Upload no front-end:** permitido apenas quando há uma sessão ativa.
16. **`UploadZone`:** recebe o arquivo e renderiza estados; validação e HTTP no `useUpload`.
17. **Componente `App`:** apenas compõe a interface e consome Custom Hooks — sem estado próprio.
18. **Erros padronizados:** `ProblemDetail` (RFC 9457) com campos `status`, `title` e `detail`.
19. **AttachmentController:** recebe `MultipartFile` e converte para objeto de entrada independente de HTTP antes de chamar o service.
20. **AttachmentService:** não recebe `MultipartFile` — recebe nome, tipo MIME, tamanho, conteúdo do arquivo e `sessionId`.
21. **GET /api/sessions/{id}/attachments:** endpoint opcional na Etapa 1, não bloqueia requisitos mínimos.

---

## Seção final

### Suposições utilizadas na especificação

- Back-end será executado em `localhost:8080`
- H2 em modo arquivo na primeira etapa
- Respostas do assistente são geradas por lógica simulada (mock)
- Sessões não possuem funcionalidade de exclusão ou edição na primeira etapa
- Upload salva arquivos no sistema de arquivos local (não em cloud storage)
- Mensagens são texto sem formatação (sem markdown ou rich text)
- Um arquivo por requisição de upload

### Decisões que precisam de validação da equipe

As decisões abaixo foram aprovadas e registradas na seção "Decisões aprovadas pela equipe" acima.

### Riscos arquiteturais identificados

- Resposta simulada pode criar expectativa equivocada no front-end sobre latência e formato da resposta real com IA
- Upload síncrono pode travar a thread em arquivos grandes — necessário considerar `CompletableFuture` ou thread pool se o limite aumentar
- Persistência de arquivos no sistema local impede escalabilidade horizontal — será necessário cloud storage no futuro
- Ausência de paginação no histórico pode ser problema com muitas mensagens

### Funcionalidades consideradas fora do escopo da primeira etapa

- Autenticação e autorização de usuários
- Deploy e infraestrutura (Docker, CI/CD)
- Integração real com API de IA (OpenAI, Claude, etc.)
- Edição e exclusão de mensagens
- WebSocket ou SSE para streaming de respostas
- Testes automatizados (serão especificados na segunda etapa)
- Upload para cloud storage (S3, etc.)
- Busca textual no histórico
- Notificações push
- Migração para PostgreSQL
