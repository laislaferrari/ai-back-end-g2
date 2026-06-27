# Plano de Implementação — Parte 2 (Pipeline RAG)

## 1. Lista de arquivos existentes relevantes

### Configuração
| Arquivo | Propósito |
|---|---|
| `pom.xml` | Dependências (Spring Boot 3.4.4, H2, JPA, Validation) |
| `src/main/resources/application.yml` | Datasource H2, JPA, multipart, porta 8080 |
| `src/main/java/com/mindjournal/config/CorsConfig.java` | CORS para `localhost:5173` |

### Controller
| Arquivo | Endpoint |
|---|---|
| `controller/HealthController.java` | `GET /api/health` |
| `controller/SessionController.java` | `POST/GET /api/sessions`, `GET /api/sessions/{id}/messages` |
| `controller/ChatController.java` | `POST /api/chat` |
| `controller/AttachmentController.java` | `POST /api/upload` (multipart) |

### DTO
| Arquivo | Tipo |
|---|---|
| `dto/ChatRequest.java` | `record ChatRequest(Long sessionId, String content)` |
| `dto/ChatResponse.java` | `record ChatResponse(MessageResponse userMessage, MessageResponse assistantMessage)` |
| `dto/AttachmentDTO.java` | `record AttachmentDTO(Long id, Long sessionId, String filename, AttachmentType type, Long size, Instant uploadDate)` |
| `dto/AttachmentInput.java` | `record AttachmentInput(String originalFilename, String mimeType, long size, byte[] content, Long sessionId)` |
| `dto/SessionResponse.java` | `record SessionResponse(Long id, String title, Instant createdAt, Instant updatedAt)` |
| `dto/MessageResponse.java` | `record MessageResponse(Long id, String content, MessageRole role, Instant timestamp)` |
| `dto/CreateSessionRequest.java` | `record CreateSessionRequest(@NotBlank @Size(max=150) String title)` |
| `dto/HealthDTO.java` | `record HealthDTO(String status, Instant timestamp)` |

### Entity
| Arquivo | Tabela |
|---|---|
| `entity/Session.java` | `journal_sessions` |
| `entity/Message.java` | `messages` |
| `entity/MessageRole.java` | `enum { USER, ASSISTANT }` |
| `entity/Attachment.java` | `attachments` |
| `entity/AttachmentType.java` | `enum { TXT, PDF }` |

### Repository
| Arquivo | Métodos |
|---|---|
| `repository/SessionRepository.java` | `findAllByOrderByUpdatedAtDesc()` |
| `repository/MessageRepository.java` | `findBySession_IdOrderByTimestampAsc(Long)` |
| `repository/AttachmentRepository.java` | (herdados de JpaRepository) |

### Service
| Arquivo | Responsabilidade |
|---|---|
| `service/SessionService.java` | CRUD de sessões, busca de mensagens |
| `service/MessageService.java` | Criação e persistência de mensagens |
| `service/ChatService.java` | Orquestra envio: salva user message, gera resposta, salva assistant message |
| `service/AiResponseGenerator.java` | Interface: `generateResponse(Long sessionId, String userMessage)` |
| `service/MockAiResponseGenerator.java` | Implementação mock com regras de palavras-chave |
| `service/AttachmentService.java` | Upload: validação de tipo/tamanho, salva em disco, persiste metadados |

### Exception
| Arquivo | HTTP Status |
|---|---|
| `exception/GlobalExceptionHandler.java` | 404 (SessionNotFound), 400 (Validation/InvalidFile), 413 (MaxUploadSizeExceeded) |
| `exception/SessionNotFoundException.java` | `RuntimeException` |
| `exception/InvalidFileException.java` | `RuntimeException` |

---

## 2. Contratos atuais do chat e upload

### Chat — `POST /api/chat`
**Request:**
```json
{ "sessionId": 1, "content": "Estou me sentindo bem hoje" }
```
**Response (201):**
```json
{
  "userMessage": { "id": 10, "content": "Estou me sentindo bem hoje", "role": "USER", "timestamp": "..." },
  "assistantMessage": { "id": 11, "content": "Que notícia excelente!...", "role": "ASSISTANT", "timestamp": "..." }
}
```
**Fluxo atual:** `ChatController` → `ChatService.sendMessage()` → salva mensagem USER → chama `AiResponseGenerator.generateResponse()` → salva mensagem ASSISTANT → retorna `ChatResponse`.

### Upload — `POST /api/upload`
**Request:** `multipart/form-data` com campos `file` (MultipartFile) + `sessionId` (Long).
**Response (201):**
```json
{
  "id": 1, "sessionId": 1, "filename": "diario.txt", "type": "TXT", "size": 1024, "uploadDate": "..."
}
```
**Fluxo atual:** `AttachmentController` → valida `file.isEmpty()` → converte para `AttachmentInput` → `AttachmentService.uploadAttachment()` → valida tamanho/tipo → salva em disco → persiste metadados → retorna `AttachmentDTO`.
**Observação:** A Parte 1 não persiste conteúdo textual parseado, não gera embeddings e não indexa chunks.

---

## 3. Mudanças mínimas necessárias

### 3.1 Infraestrutura (migração H2 → PostgreSQL + pgvector)

| O quê | Por quê |
|---|---|
| Substituir H2 por PostgreSQL no `application.yml` | pgvector exige PostgreSQL |
| Adicionar extensão pgvector via migration | Armazenar `float[]` como vetor |
| Adicionar propriedades `spring.datasource.url`, `username`, `password` via env | Não hardcode segredos |
| Adicionar dependência `org.postgresql:postgresql` no `pom.xml` | Driver JDBC |
| Manter perfil H2 para testes locais | Evitar dependência de PostgreSQL nos testes unitários |
| Adicionar `application-test.yml` com H2 | Isolar testes |

### 3.2 Novas entidades

| Entidade | Tabela | Campos principais |
|---|---|---|
| `Document` | `documents` | id, originalFilename, contentType, size, status (enum), errorMessage, createdAt, updatedAt |
| `DocumentChunk` | `document_chunks` | id, documentId (FK), content, chunkIndex, embedding (vector), createdAt |
| `DocumentStatus` | — | enum: RECEIVED, PROCESSING, INDEXED, FAILED |

### 3.3 Novos DTOs

| DTO | Propósito |
|---|---|
| `DocumentResponse` | Resposta com dados do documento |
| `DocumentStatusResponse` | Resposta `GET /api/documents/{id}/status` |
| `SourceDTO` | Fonte retornada no chat: documentId, documentName, chunkId, chunkIndex, content, similarity |
| `ChatSourceResponse` | Novo record que estende `ChatResponse` com `List<SourceDTO> sources` |
| `DocumentIndexedWebhook` | Payload do webhook n8n |

### 3.4 Novas interfaces e serviços

| Interface/Serviço | Responsabilidade |
|---|---|
| `DocumentParser` (interface) | `boolean supports(contentType, filename)`, `String parse(byte[], contentType, filename)` |
| `TxtDocumentParser` | Implementa `DocumentParser` para `text/plain` |
| `PdfDocumentParser` | Implementa `DocumentParser` para `application/pdf` |
| `TextChunker` (classe) | `List<String> split(String text)` — configurável via chunkSize + chunkOverlap |
| `EmbeddingService` (interface) | `List<Float> embed(String text)`, `List<List<Float>> embedAll(List<String>)` |
| `OpenAiEmbeddingService` | Implementa `EmbeddingService` via API OpenAI (chave via env) |
| `DocumentIngestionService` | Orquestra: parser → chunking → embedding → persistência → webhook |
| `RagService` | `RagContext retrieve(question)` — embedding da pergunta → busca vetorial → formata contexto |
| `N8nWebhookClient` | Notifica n8n após indexação |

### 3.5 Mudanças em arquivos existentes

| Arquivo | Mudança |
|---|---|
| `pom.xml` | Adicionar `postgresql`, `spring-boot-starter-test`, `spring-boot-testcontainers`, `testcontainers-postgresql` |
| `application.yml` | Adicionar `rag.*` e `n8n.*` config props; preparar datasource PostgreSQL (ativo por profile `postgres`) |
| `ChatService.java` | Injetar `RagService`; passar `sources` no prompt do assistente; incluir `sources` no `ChatResponse` |
| `ChatResponse.java` | Adicionar `List<SourceDTO> sources` (default `[]`) |
| `AttachmentService.java` | Após salvar attachment, disparar pipeline de ingestão chamando `DocumentIngestionService` |
| `GlobalExceptionHandler.java` | Adicionar handler para `DocumentNotFoundException` (404), `DocumentStateException` (409), `IngestionException` (500) |
| `CorsConfig.java` | Nenhuma mudança necessária (já cobre `/api/**`) |

### 3.6 Novos controllers

| Controller | Endpoints |
|---|---|
| `DocumentController` | `POST /api/documents/{id}/ingest` (disparar ingestão), `GET /api/documents/{id}/status`, `GET /api/documents/{id}` |
| *Alternativa:* estender `AttachmentController` com rota de ingestão | Menos arquivos, mas mistura responsabilidades |

### 3.7 Novos repositories

| Repository | Consultas |
|---|---|
| `DocumentRepository` | `findByStatus(DocumentStatus)` |
| `DocumentChunkRepository` | `findByDocumentIdOrderByChunkIndex(Long)`, `findTopKByEmbeddingSimilarity()` (native query com pgvector) |

### 3.8 Novas exceções

| Exceção | HTTP | Quando |
|---|---|---|
| `DocumentNotFoundException` | 404 | Documento não encontrado |
| `DocumentStateException` | 409 | Tentativa de reindexar documento já PROCESSING ou INDEXED |
| `IngestionException` | 500 | Falha não recuperável na ingestão |
| `AiServiceException` | 502 | Falha no serviço de embedding externo |

### 3.9 Nova config class

| Classe | Propriedades |
|---|---|
| `RagProperties` (`@ConfigurationProperties("rag")`) | `topK`, `minSimilarity`, `chunkSize`, `chunkOverlap` |
| `N8nProperties` (`@ConfigurationProperties("n8n")`) | `webhookUrl` |

---

## 4. Ordem de implementação

```
Fase 0 — Plano e documentação
  └── docs/parte-2/implementation-plan.md (este arquivo)

Fase 1 — Infraestrutura (branch: feature/postgres-pgvector)
  1. Adicionar dependências PostgreSQL ao pom.xml
  2. Criar application-postgres.yml com datasource PostgreSQL + env vars
  3. Criar RagProperties e N8nProperties
  4. Adicionar script SQL de migração (criar extensão pgvector, tabelas documents/document_chunks)
  5. Adicionar uploads/ ao .gitignore se necessário
  6. Testar: subir com PostgreSQL, verificar health

Fase 2 — Domínio Document + DocumentChunk (branch: feature/document-domain)
  1. Criar enum DocumentStatus
  2. Criar entidade Document
  3. Criar entidade DocumentChunk (com campo embedding vector)
  4. Criar DocumentRepository
  5. Criar DocumentChunkRepository (query de similaridade)
  6. Criar DocumentNotFoundException, DocumentStateException, IngestionException
  7. Criar DocumentResponse, DocumentStatusResponse
  8. Testar: persistir documento via teste de integração

Fase 3 — Parsing e Chunking (branch: feature/document-parsing + feature/document-chunking)
  1. Criar interface DocumentParser
  2. Criar TxtDocumentParser
  3. Criar PdfDocumentParser (Apache PDFBox no pom.xml)
  4. Criar TextChunker (configurável via RagProperties)
  5. Testar: parse de .txt conhecido, parse de .pdf conhecido, chunking com tamanho controlado

Fase 4 — Embedding (branch: feature/embedding-service)
  1. Criar interface EmbeddingService
  2. Criar OpenAiEmbeddingService (chave via env AI_API_KEY)
  3. Criar AiServiceException
  4. Testar: embed com texto conhecido (pode exigir mock)

Fase 5 — Pipeline de ingestão (branch: feature/document-ingestion)
  1. Criar DocumentIngestionService
  2. Criar N8nWebhookClient (com fallback silencioso se URL vazia)
  3. Criar DocumentIndexedWebhook DTO
  4. Modificar AttachmentService para chamar ingestão após upload
  5. Criar DocumentController (endpoints de ingestão e status)
  6. Adicionar handlers no GlobalExceptionHandler
  7. Testar: upload → ingestão completa → status INDEXED

Fase 6 — Retrieval e fontes no chat (branch: feature/rag-retrieval)
  1. Criar RagService
  2. Criar SourceDTO
  3. Modificar ChatResponse para incluir sources (List<SourceDTO>)
  4. Modificar ChatService para injetar RagService e passar contexto + sources
  5. Manter MockAiResponseGenerator funcional (pode receber contexto extra)
  6. Testar: pergunta com match → sources preenchidas; pergunta sem match → sources vazio

Fase 7 — n8n webhook (branch: feature/n8n-webhook)
  1. Implementar N8nWebhookClient (RestClient ou WebClient)
  2. Configurar webhook URL via env
  3. Testar: evento disparado após indexação

Fase 8 — Testes (branch: feature/rag-tests)
  1. Testes unitários: DocumentParser, TextChunker, RagService (mock EmbeddingService)
  2. Testes de integração: fluxo completo upload → ingestão → chat com sources
  3. Testes de contrato: resposta do chat mantém compatibilidade (sources opcional)

Fase 9 — Documentação final (branch: feature/docs-part-2)
  1. Atualizar AGENTS.md com novos arquivos/endpoints
  2. Atualizar docs/specs/01-backend-architecture.md
  3. Preencher docs/parte-2/03-checklist-entrega.md
```

---

## 5. Riscos de quebra e mitigações

| Risco | Impacto | Mitigação |
|---|---|---|
| Mudar `ChatResponse` (adicionar `sources`) | Frontend pode ignorar campo extra | `sources` é opcional (`default []`); JSON ignorará campo desconhecido |
| Migrar H2 → PostgreSQL | Consultas ou tipos incompatíveis | Manter H2 como perfil `default`; PostgreSQL como perfil `postgres`; testar ambos |
| Dependência externa (OpenAI API) | Falha de embedding derruba chat | `AiServiceException` → 502; chat funciona sem fontes se RagService falhar |
| PDFBox adiciona ~5 MB ao JAR | Tamanho do artefato | Dependência inevitável; impacto aceitável |
| n8n webhook fora do ar | Latência na ingestão | `N8nWebhookClient` com timeout curto + fallback silencioso; não trava o fluxo |
| Upload existente (Parte 1) não chama ingestão | Documentos antigos não indexados | Apenas novos uploads disparam ingestão; retrocompatibilidade mantida |
| `AttachmentService` atualmente não extrai texto | Conteúdo do arquivo não está disponível | `AttachmentService.uploadAttachment()` precisa expor o conteúdo byte[] para `DocumentIngestionService` |

---

## 6. Compatibilidade com o frontend atual

### Contratos preservados
- `GET /api/health` — inalterado.
- `POST /api/sessions`, `GET /api/sessions`, `GET /api/sessions/{id}`, `GET /api/sessions/{id}/messages` — inalterados.
- `POST /api/upload` — retorno `AttachmentDTO` inalterado; a indexação ocorre de forma assíncrona (ou síncrona controlada por transaction) sem impacto na response.
- `POST /api/chat` — **única mudança contratual**: `ChatResponse` ganha campo `sources` (`List<SourceDTO>`). O frontend existente que não lê `sources` simplesmente ignora o campo extra (JSON permissivo).

### Compatibilidade do `ChatResponse`
```java
// ANTES (Parte 1)
public record ChatResponse(MessageResponse userMessage, MessageResponse assistantMessage) {}

// DEPOIS (Parte 2)
public record ChatResponse(
    MessageResponse userMessage,
    MessageResponse assistantMessage,
    List<SourceDTO> sources          // novo campo, default []
) {}
```
O frontend TypeScript atual define:
```typescript
export interface ChatResponse {
  userMessage: Message;
  assistantMessage: Message;
}
```
Após a mudança, o campo `sources` será ignorado silenciosamente pelo frontend até que os tipos sejam atualizados. A interface `ChatResponse` no frontend pode ganhar `sources?: Source[]` sem quebra.

### Novos endpoints (não afetam frontend atual)
- `POST /api/documents/{id}/ingest` — usado internamente ou por script/n8n, não pelo chat.
- `GET /api/documents/{id}/status` — consultado pelo frontend futuramente ou pelo n8n.
- `GET /api/documents/{id}` — detalhes do documento.

---

## 7. Proposta de testes

### 7.1 Testes unitários

| Classe | Teste | O que verificar |
|---|---|---|
| `TxtDocumentParser` | `supports txt` | Retorna `true` para `text/plain` e `.txt` |
| `TxtDocumentParser` | `supports pdf` | Retorna `false` para `application/pdf` |
| `TxtDocumentParser` | `parse txt content` | Extrai conteúdo textual corretamente |
| `PdfDocumentParser` | `supports pdf` | Retorna `true` para `application/pdf` e `.pdf` |
| `PdfDocumentParser` | `parse pdf content` | Extrai texto de PDF conhecido |
| `TextChunker` | `split short text` | Retorna 1 chunk se texto < chunkSize |
| `TextChunker` | `split long text` | Divide corretamente com overlap |
| `TextChunker` | `split empty text` | Retorna lista vazia |
| `RagService` | `retrieve with match` | Retorna `RagContext` com fontes (mock `EmbeddingService` + `DocumentChunkRepository`) |
| `RagService` | `retrieve no match` | Retorna `RagContext` com lista vazia (mock retorna lista vazia) |
| `RagService` | `retrieve embedding failure` | Propaga `AiServiceException` |
| `ChatService` | `sendMessage with sources` | `ChatResponse.sources` populado (mock `RagService`) |
| `ChatService` | `sendMessage no sources` | `ChatResponse.sources` vazio (mock `RagService` retorna vazio) |
| `N8nWebhookClient` | `notify success` | RestClient chamado com payload correto (mock) |
| `N8nWebhookClient` | `notify url empty` | Não lança exceção (fallback silencioso) |
| `OpenAiEmbeddingService` | `embed text` | Retorna lista de floats (mock RestClient) |

### 7.2 Testes de integração

| Cenário | Passos | Verificação |
|---|---|---|
| Upload + ingestão completa (H2) | `POST /api/upload` com `.txt` conhecido → aguardar processamento → `GET /api/documents/{id}/status` | Status = `INDEXED`; chunks persistidos |
| Upload + ingestão com PDF | `POST /api/upload` com `.pdf` → `GET /api/status` | Status = `INDEXED` |
| Upload com arquivo inválido | `POST /api/upload` com `.exe` | HTTP 400 |
| Chat com fontes | Upload de .txt com conteúdo conhecido → indexar → `POST /api/chat` perguntando sobre o conteúdo | `sources` não vazio |
| Chat sem fontes | `POST /api/chat` perguntando sobre conteúdo inexistente | `sources` vazio |
| Health check | `GET /api/health` | HTTP 200, status "UP" |
| Sessão inexistente | `POST /api/chat` com `sessionId` inválido | HTTP 404 |

### 7.3 Estratégia de perfis para testes
- **Profile `test`** (default): H2 em memória + Mock do `EmbeddingService` para testes unitários e de integração rápidos.
- **Profile `postgres`**: PostgreSQL real com pgvector para testes de aceitação locais ou em CI (via Testcontainers).
- Mock do `EmbeddingService` retorna vetor fixo de tamanho conhecido (ex: 1536 dimensões) para previsibilidade.

---

## 8. Resumo dos arquivos

### Arquivos a criar (16)

```
src/main/java/com/mindjournal/
├── config/
│   ├── RagProperties.java                         // @ConfigurationProperties("rag")
│   └── N8nProperties.java                         // @ConfigurationProperties("n8n")
├── controller/
│   └── DocumentController.java                    // POST /documents/{id}/ingest, GET /documents/{id}/status, GET /documents/{id}
├── dto/
│   ├── DocumentResponse.java                      // record
│   ├── DocumentStatusResponse.java                // record
│   ├── SourceDTO.java                             // record
│   └── DocumentIndexedWebhook.java                // record (payload n8n)
├── entity/
│   ├── Document.java                              // @Entity, table documents
│   ├── DocumentChunk.java                         // @Entity, table document_chunks (vector field)
│   └── DocumentStatus.java                        // enum { RECEIVED, PROCESSING, INDEXED, FAILED }
├── exception/
│   ├── DocumentNotFoundException.java             // 404
│   ├── DocumentStateException.java                // 409
│   ├── IngestionException.java                    // 500
│   └── AiServiceException.java                    // 502
├── repository/
│   ├── DocumentRepository.java                    // JpaRepository<Document, Long>
│   └── DocumentChunkRepository.java               // JpaRepository<DocumentChunk, Long> + query similaridade
├── service/
│   ├── DocumentParser.java                        // interface
│   ├── TxtDocumentParser.java                     // implements DocumentParser
│   ├── PdfDocumentParser.java                     // implements DocumentParser
│   ├── TextChunker.java                           // split com chunkSize + overlap
│   ├── EmbeddingService.java                      // interface
│   ├── OpenAiEmbeddingService.java                // implements EmbeddingService
│   ├── DocumentIngestionService.java              // orquestra parser → chunk → embed → persist → webhook
│   ├── RagService.java                            // retrieve contexto + fontes
│   └── N8nWebhookClient.java                      // notifica n8n

src/main/resources/
├── application-postgres.yml                       // Profile PostgreSQL
├── application-test.yml                           // Profile test (H2 em memória)
└── db/migration/
    └── V1__create_pgvector_extension.sql           // CREATE EXTENSION vector + CREATE TABLE documents + document_chunks

src/test/java/com/mindjournal/
├── service/
│   ├── TxtDocumentParserTest.java
│   ├── PdfDocumentParserTest.java
│   ├── TextChunkerTest.java
│   ├── RagServiceTest.java
│   ├── ChatServiceTest.java
│   └── N8nWebhookClientTest.java
├── controller/
│   └── DocumentControllerTest.java
└── integration/
    └── DocumentIngestionFlowTest.java
```

### Arquivos a modificar (8)

```
pom.xml                                          // +postgresql, +pdfbox, +testcontainers, +spring-boot-starter-test
src/main/resources/application.yml                // +rag.*, +n8n.*, +profile postgres
src/main/java/com/mindjournal/
├── service/
│   ├── ChatService.java                          // +RagService, monta prompt com contexto, inclui sources no ChatResponse
│   └── AttachmentService.java                    // após salvar attachment, chama DocumentIngestionService
├── dto/ChatResponse.java                         // +List<SourceDTO> sources (default [])
├── controller/
│   └── AttachmentController.java                 // pode precisar expor o sessionId + file bytes para serviço de ingestão
└── exception/GlobalExceptionHandler.java          // +DocumentNotFoundException, +DocumentStateException, +IngestionException, +AiServiceException
```

### Total
- **Criados:** 27 arquivos (16 Java + 3 resources/config + 1 migration SQL + 7 testes)
- **Modificados:** 8 arquivos existentes
- **Preservados sem alteração:** todos os demais (HealthController, SessionController, SessionService, MessageService, Session, Message, MessageRole, Attachment, AttachmentType, AttachmentRepository, SessionRepository, MessageRepository, AiResponseGenerator, MockAiResponseGenerator, CorsConfig, e todos os DTOs existentes exceto ChatResponse)

---

## 9. Observações finais

1. `MockAiResponseGenerator` continua funcionando — a interface `AiResponseGenerator.generateResponse()` pode receber o contexto RAG como segundo parâmetro ou o `ChatService` concatena o contexto antes de chamar o gerador, evitando quebra de contrato da interface.
2. A migração de H2 para PostgreSQL deve ser feita via **profile** (`spring.profiles.active=postgres`), mantendo H2 como profile padrão para desenvolvimento leve.
3. Nenhum endpoint existente é removido ou renomeado.
4. Nenhum segredo é hardcoded — chave de embedding e URL de webhook vêm de variáveis de ambiente.
5. `DocumentIngestionService` não acessa banco diretamente — usa `DocumentRepository` e `DocumentChunkRepository`.
6. `RagService` não acessa banco diretamente — usa `DocumentChunkRepository` para consulta de similaridade.
7. Controllers não fazem parsing — apenas delegam para services.
