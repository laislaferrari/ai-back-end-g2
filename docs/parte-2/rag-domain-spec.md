# Especificação do Domínio RAG — MindJourney IA

## 1. Visão geral

Esta especificação define o domínio RAG (Retrieval-Augmented Generation) para o MindJourney IA. O objetivo é permitir que arquivos enviados pelo usuário via upload sejam processados: extração de texto, chunking, geração de embeddings e indexação vetorial, para que o chat possa consultar semanticamente o conteúdo dos documentos.

O escopo é estritamente aditivo. Nenhum endpoint, contrato ou funcionalidade da Parte 1 é alterado. O upload existente (`POST /api/upload`) continua funcionando sem modificações.

---

## 2. Diagnóstico do código atual

### 2.1 Classes e contratos reutilizáveis

| Classe / Contrato | Papel no RAG |
|---|---|
| `Attachment` / `attachments` | Metadados do arquivo TXT/PDF — fonte de verdade para nome, tipo, tamanho, caminho e sessão |
| `AttachmentService` | Pipeline de validação, armazenamento em disco e persistência de metadados do upload |
| `AttachmentController` (`POST /api/upload`) | Endpoint de upload existente — permanece inalterado |
| `GlobalExceptionHandler` | `ProblemDetail` padronizado — reutilizado nas novas exceções |
| `InvalidFileException` | Modelo para novas exceções de domínio |
| `SessionNotFoundException` | Padrão de mensagem descritiva |
| `MessageService.createAndSaveMessage` | Persistência de mensagens com role e timestamp |
| `ChatService` | Orquestração do chat — futuro ponto de injeção da busca RAG |
| `AiResponseGenerator` | Interface do gerador de resposta — pode receber `RagContext` como parâmetro adicional |
| `CorsConfig` | Liberação de `/api/**` — novos endpoints já cobertos |

### 2.2 Fluxo atual de upload

```
POST /api/upload (multipart: file + sessionId)
  -> AttachmentController.uploadFile()
    -> valida file.isEmpty()
    -> converte MultipartFile -> AttachmentInput (byte[] content)
    -> AttachmentService.uploadAttachment()
      -> valida tamanho (max 10MB)
      -> valida tipo/extensao (.txt + text/plain | .pdf + application/pdf)
      -> busca Session por sessionId
      -> salva arquivo em disco
      -> cria Attachment com session, filename, type, size, filePath, uploadDate
      -> atualiza session.updatedAt
    -> retorna AttachmentDTO (201 Created)
```

### 2.3 Como Attachment funciona hoje

`Attachment` é uma entidade JPA (`attachments`) que representa metadados de um arquivo enviado: nome original, tipo (TXT/PDF), tamanho em bytes, caminho físico no disco e data de upload. Possui `@ManyToOne` para `Session`. O conteúdo do arquivo está apenas no sistema de arquivos. Não há extração de texto nem indexação.

### 2.4 Lacunas para o RAG

1. Conteúdo textual não extraído — Attachment não armazena nem referencia o texto.
2. Sem rastreamento de processamento — não há status de indexação.
3. Sem embeddings — nenhuma coluna vector, nenhum chunk.
4. Sem idempotência de processamento — não é possível saber se um Attachment já foi indexado.

---

## 3. Decisões arquiteturais

### 3.1 Relação Document -> Attachment: obrigatória e exclusiva

**Document possui `attachment_id` como NOT NULL e UNIQUE.**

| Aspecto | Decisão |
|---|---|
| Navegabilidade | `Document -> Attachment` via `@OneToOne`, FK `attachment_id` |
| Obrigatoriedade | `attachment_id` NOT NULL — todo Document representa a indexação de exatamente um Attachment |
| Exclusividade | `UNIQUE(attachment_id)` — um Attachment só pode ser indexado uma vez |
| Cascade | `ON DELETE CASCADE` na FK — excluir o Attachment remove o Document e seus chunks |
| Remoção inversa | Excluir o Document **não** remove o Attachment (preserva o arquivo original para reindexação futura) |
| Responsabilidade do Document | Apenas estado da indexação: `status`, `errorMessage`, `createdAt`, `updatedAt` |
| Responsabilidade do Attachment | Metadados do arquivo: `filename`, `type`, `size`, `filePath`, `session` |
| Documento avulso | Fora do escopo — não há Document sem Attachment nesta versão |

**Impacto na Parte 1:** nenhum. Nenhuma classe existente é alterada. O upload `POST /api/upload` continua criando `Attachment` como antes.

### 3.2 Separação de responsabilidades

- **Controller**: endpoints REST, validação, delega a serviços.
- **Service**: regras de negócio, orquestração, fronteiras transacionais.
- **Repository**: acesso a dados, consultas de similaridade vetorial.
- **Entity / DTO**: modelos de dados.

Nenhum controller chama parser, chunker ou embedding. `DocumentIngestionService` é o único orquestrador. `RagService` não acessa banco diretamente.

### 3.3 Pacotes novos

```
com.mindjournal.service.rag
com.mindjournal.service.parsing
com.mindjournal.service.chunking
com.mindjournal.service.embedding
com.mindjournal.service.webhook
```

### 3.4 EmbeddingService agnóstico a provedor

`EmbeddingService` é interface no pacote `com.mindjournal.service.embedding`. Nenhuma classe de domínio importa classes de OpenAI, Ollama ou qualquer provedor externo. O domínio conhece apenas `float[]`.

### 3.5 n8n permanece externo

`N8nWebhookClient` faz requisição HTTP POST para URL configurável. Nenhuma regra de negócio do MindJourney é implementada no n8n.

---

## 4. Modelo de entidades

### 4.1 Document

**Responsabilidade:** Representar exclusivamente o estado da indexação RAG de um Attachment.

| Campo | Tipo Java | Tipo PostgreSQL | Nulabilidade | Observação |
|---|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | NOT NULL | PK |
| `attachment` | `Attachment` | `BIGINT` (FK) | NOT NULL, UNIQUE | FK -> `attachments(id)` ON DELETE CASCADE |
| `status` | `DocumentStatus` (enum) | `VARCHAR(20)` | NOT NULL | `RECEIVED`, `PROCESSING`, `INDEXED`, `FAILED` |
| `errorMessage` | `String` | `TEXT` | NULLABLE | Preenchido apenas se status = `FAILED` |
| `createdAt` | `Instant` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | `@PrePersist` |
| `updatedAt` | `Instant` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | `@PreUpdate` |

**Regras de negócio:**
- Status nunca é nulo.
- `errorMessage` só pode ser preenchido quando `status = FAILED`.
- `attachment_id` é obrigatório e único.
- Metadados do arquivo (fileName, contentType, size, filePath, session) são obtidos exclusivamente via `document.getAttachment()`.

### 4.2 DocumentStatus (enum)

```java
public enum DocumentStatus {
    RECEIVED,
    PROCESSING,
    INDEXED,
    FAILED
}
```

Transições válidas:
- `RECEIVED` -> `PROCESSING`
- `PROCESSING` -> `INDEXED`
- `PROCESSING` -> `FAILED`
- `FAILED` -> `RECEIVED` (reprocessamento)
- `INDEXED` -> `RECEIVED` (reprocessamento)

### 4.3 DocumentChunk

**Responsabilidade:** Armazenar fragmento de texto extraído com embedding.

| Campo | Tipo Java | Tipo PostgreSQL | Nulabilidade | Observação |
|---|---|---|---|---|
| `id` | `Long` | `BIGSERIAL` | NOT NULL | PK |
| `document` | `Document` | `BIGINT` (FK) | NOT NULL | FK -> `documents(id)` ON DELETE CASCADE |
| `content` | `String` | `TEXT` | NOT NULL | Texto do fragmento |
| `chunkIndex` | `Integer` | `INTEGER` | NOT NULL | Ordem sequencial, iniciando em 0 |
| `embedding` | a decidir | `vector(D)` | NOT NULL | Dimensão D depende do modelo |
| `createdAt` | `Instant` | `TIMESTAMP WITH TIME ZONE` | NOT NULL | `@PrePersist` |

**Regras:**
- Cascade total: deletar Document remove todos os chunks.
- `content` não pode ser vazio.
- `embedding` nunca é armazenado sem o respectivo content.

**Constraints:**
- UK: `(document_id, chunk_index)`
- FK: `document_id` -> `documents(id)` ON DELETE CASCADE

**Índice vetorial:** decisão pendente (ver seção 16).

---

## 5. Diagrama textual de relacionamentos

```
attachments (Parte 1 - inalterado)
  |  (1)
  |  UNIQUE
  v
documents (NOVO)
  - id (PK)
  - attachment_id (FK NOT NULL UNIQUE -> attachments, ON DELETE CASCADE)
  - status
  - error_message (nullable)
  - created_at
  - updated_at
  |
  | 1:N (CASCADE DELETE)
  v
document_chunks (NOVO)
  - document_id (FK)
  - chunk_index (UK com document_id)
  - content (TEXT)
  - embedding (vector(D))
  - created_at
```

Attachment -> Document: 1 para 0..1 (exclusivo, opcional do lado do Attachment).
Document -> Attachment: 1 para 1 (obrigatório).

---

## 6. Contratos internos

### 6.1 DocumentParser

**Responsabilidade:** Extrair texto puro de conteúdo binário.

**Entrada:** `byte[] content, String contentType`

**Saída:** `String` (texto puro)

**Assinatura conceitual:**

```java
String parse(byte[] content, String contentType);
```

**Regras:**
- `text/plain` -> `new String(content, StandardCharsets.UTF_8)`.
- `application/pdf` -> extração por biblioteca externa (decisão pendente).
- Texto vazio -> lança `EmptyDocumentException`.

### 6.2 TextChunker

**Responsabilidade:** Fragmentar texto com overlap configurável.

**Entrada:** `String text`

**Saída:** `List<String>`

**Assinatura conceitual:**

```java
List<String> chunk(String text);
```

**Regras:**
- Tamanho e overlap em propriedades externas.
- Texto menor que chunk size -> lista com um elemento.

### 6.3 EmbeddingService (interface)

**Responsabilidade:** Converter texto em vetor.

**Entrada:** `String text`

**Saída:** `float[]`

**Assinatura conceitual:**

```java
float[] generateEmbedding(String text);
```

**Regras:**
- Interface no pacote `com.mindjournal.service.embedding`.
- Implementação injetada por profile/property.
- Dimensão em configuração externa.
- Falha externa -> `EmbeddingException`.

### 6.4 DocumentIngestionService

**Responsabilidade:** Orquestrar pipeline de ingestão com fronteiras transacionais curtas.

**Entrada:** `Long documentId`

**Saída:** `void`

**Assinatura conceitual:**

```java
void ingest(Long documentId);
```

**Fronteiras transacionais — serviços em bean separado (IngestionTransactionService) para evitar self-invocation:**

```
1. ingestionTransactionService.markAsProcessing(documentId)
   [TX curta: status -> PROCESSING, commit ao retornar]

2. (fora de transacao)
   - ler byte[] do arquivo
   - DocumentParser.parse -> texto
   - TextChunker.chunk -> fragmentos
   - EmbeddingService.generateEmbedding (para cada fragmento) -> float[]
   - preparar List<DocumentChunk> em memoria

3. ingestionTransactionService.replaceChunksAndMarkIndexed(documentId, preparedChunks)
   [TX curta: remover chunks antigos, inserir novos, status -> INDEXED, commit ao retornar]

4. Se 3 falhar (excecao):
   - rollback ja ocorreu, chunks antigos preservados
   - ingestionTransactionService.markAsFailed(documentId, safeMessage)
     [TX em REQUIRES_NEW: status -> FAILED, commit ao retornar]
```

### 6.5 IngestionTransactionService (bean separado)

Métodos públicos com `@Transactional`:

```java
// Transacao 1: marca PROCESSING
void markAsProcessing(Long documentId);

// Transacao 2: substitui chunks atomico
void replaceChunksAndMarkIndexed(Long documentId, List<DocumentChunk> preparedChunks);

// Transacao de recuperacao: REQUIRES_NEW
void markAsFailed(Long documentId, String safeMessage);
```

O commit ocorre quando cada método transacional retorna com sucesso.

### 6.6 DocumentRepository

```java
Optional<Document> findById(Long id);
List<Document> findAllByOrderByCreatedAtDesc();
List<Document> findByStatus(DocumentStatus status);
boolean existsByAttachmentId(Long attachmentId);  // para validacao 409
```

### 6.7 DocumentChunkRepository

```java
List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

void deleteByDocumentId(Long documentId);  // usado em reprocessamento

List<ChunkWithScore> findSimilarChunks(
    Long sessionId,
    float[] embedding,
    int topK,
    double minSimilarity
);
```

Consulta conceitual:

```sql
SELECT
    dc.*,
    1 - (dc.embedding <=> :embedding) AS score
FROM document_chunks dc
JOIN documents d ON d.id = dc.document_id
JOIN attachments a ON a.id = d.attachment_id
WHERE a.session_id = :sessionId
  AND 1 - (dc.embedding <=> :embedding) >= :minSimilarity
ORDER BY dc.embedding <=> :embedding
LIMIT :topK
```

O JOIN com `documents` e `attachments` garante que a busca considere apenas chunks de documentos pertencentes à sessão informada. Isso impede que o chat de uma sessão recupere documentos indexados em outra sessão.

**ChunkWithScore** é um DTO interno:

```java
public record ChunkWithScore(
    DocumentChunk chunk,
    Double score
) {}
```

A similaridade é `1 - (embedding <=> :embedding)`, conforme o operador `<=>` definido pelo trabalho. `TOP_K` e `MIN_SIMILARITY` são aplicados na SQL, não em memória.

### 6.8 RagService

**Responsabilidade:** Busca semântica e preparação de contexto para o chat.

**Entrada:** `String question, Long sessionId`

**Saída:** `RagContext`

**Assinatura conceitual:**

```java
RagContext retrieve(String question, Long sessionId);
```

**RagContext:**

```java
public record RagContext(
    String context,           // Texto formatado para o prompt
    List<RagSource> sources   // Fontes com score
) {}
```

**RagSource:**

```java
public record RagSource(
    Long documentId,
    Long attachmentId,
    String fileName,
    String content,
    Integer chunkIndex,
    Double score
) {}
```

**Regras:**
- Gera embedding da pergunta via `EmbeddingService`.
- Chama `DocumentChunkRepository.findSimilarChunks(sessionId, embedding, topK, minSimilarity)`.
- Enriquece cada `ChunkWithScore` com dados do `Document` e `Attachment` (fileName).
- Formata `context` concatenando conteúdos dos chunks.
- **Não acessa banco diretamente** — delega ao repository.
- **Não faz parsing, chunking ou ingestão.**

### 6.9 N8nWebhookClient

**Responsabilidade:** Notificar n8n sobre indexação concluída, disparado **somente após commit** da transação `replaceChunksAndMarkIndexed`.

**Entrada:** `Document document`

**Saída:** `void`

**Estratégia pós-commit unificada:**

1. `replaceChunksAndMarkIndexed(documentId, preparedChunks)` dentro de `IngestionTransactionService`:
   - persiste os novos chunks
   - remove os antigos (se reprocessamento)
   - atualiza status para `INDEXED`
   - **publica** `DocumentIndexedEvent` via `ApplicationEventPublisher` ainda dentro da transação
   - retorna

2. Um listener separado:

```java
@Component
public class DocumentIndexedEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentIndexed(DocumentIndexedEvent event) {
        n8nWebhookClient.notify(event.getDocument());
    }
}
```

3. Se a transação sofrer rollback, o listener `AFTER_COMMIT` não dispara.
4. Se o commit for bem-sucedido, o listener dispara e chama o webhook.

**Comportamento em falha:**
- Timeout configurável (ex.: 5000ms — valor exemplificativo).
- Falha HTTP: log WARN, não altera INDEXED, não remove chunks.
- Pode ser tentado novamente por mecanismo externo (n8n consulta `GET /api/documents/{id}/status`).

---

## 7. Fluxo de ingestão

```
0. POST /api/upload (multipart) -> Attachment (pre-existente, Parte 1)
       |
1. POST /api/documents (JSON: {"attachmentId": 1})
   -> DocumentController
     -> valida attachmentId presente
     -> busca Attachment (404 se inexistente)
     -> verifica se ja existe Document para o Attachment (409 se existir)
     -> DocumentService.createDocument(attachment)
       -> cria Document com status = RECEIVED e attachment vinculado
       -> retorna DocumentResponse (201)
   |
2. DocumentIngestionService.ingest(documentId) [assincono]
   |
3. markAsProcessing(documentId)
   [TX 1] status -> PROCESSING, commit
   |
4. (fora de transacao)
   - le byte[] via attachment.getFilePath()
   - DocumentParser.parse(bytes, contentType) -> texto
   - TextChunker.chunk(texto) -> List<String>
   - para cada fragmento: EmbeddingService.generateEmbedding -> float[]
   - prepara List<DocumentChunk> em memoria
   |
5. replaceChunksAndMarkIndexed(documentId, preparedChunks)
   [TX 2] remove chunks antigos (se reprocessamento), insere novos, status -> INDEXED, commit
          publica DocumentIndexedEvent dentro da transacao
   |
6. [APOS COMMIT -- TransactionalEventListener(AFTER_COMMIT)]
   N8nWebhookClient.notify(document)
   (falha nao aborta nada, apenas log)
```

---

## 8. Fluxos de falha

### 8.1 Falha de parsing
- `DocumentParser` lança `ParsingException` ou `EmptyDocumentException`.
- `DocumentIngestionService` captura, chama `markAsFailed(documentId, mensagem)` em transação `REQUIRES_NEW`.

### 8.2 Falha no chunking
- Considerada falha de programa. `markAsFailed`.

### 8.3 Falha na geração de embeddings
- Vetores são gerados em memória. Se um falha, toda a preparação é abortada.
- Nenhum chunk chega ao banco.
- `markAsFailed`.

### 8.4 Falha de persistência (TX 2 — replaceChunksAndMarkIndexed)
- Rollback. Document permanece em PROCESSING.
- `DocumentIngestionService` captura a exceção **imediatamente**.
- Chama `markAsFailed(documentId, safeMessage)` em transação `REQUIRES_NEW`.
- Chunks antigos (se reprocessamento) foram restaurados pelo rollback.
- Um job de recuperação para documentos presos em PROCESSING pode existir apenas como fallback operacional, não como mecanismo principal.

### 8.5 Falha no webhook
- Não afeta INDEXED. Log WARN.

### 8.6 Reprocessamento
- `POST /api/documents/{id}/reprocess` altera status para `RECEIVED` (se status for FAILED ou INDEXED).
- `ingest()` executa novamente:
  - Nova preparação em memória (parsing, chunking, embeddings).
  - `replaceChunksAndMarkIndexed`: dentro de uma única transação, remove chunks antigos, insere novos, atualiza status para INDEXED.
  - Se falhar, rollback restaura chunks antigos + `markAsFailed` em REQUIRES_NEW.

### 8.7 Indexar documento já INDEXED ou PROCESSING
- `RECEIVED` -> alimenta ingestão.
- `INDEXED` -> idempotente (retorna sem ação) ou 409 se chamado via controller.
- `PROCESSING` -> 409 no controller.

---

## 9. Contratos HTTP

### 9.1 POST /api/documents

**Responsabilidade:** Solicitar a indexação RAG de um Attachment já enviado.

**Request:**
```json
{
  "attachmentId": 1
}
```

**Validações:**
- `attachmentId` presente e válido.
- Attachment existe (senão 404).
- Nenhum Document vinculado a este Attachment (senão 409).

**Response (201):**
```json
{
  "id": 1,
  "attachmentId": 1,
  "sessionId": 3,
  "fileName": "relatorio.txt",
  "contentType": "text/plain",
  "size": 2048,
  "status": "RECEIVED",
  "createdAt": "2026-06-27T10:00:00Z",
  "updatedAt": "2026-06-27T10:00:00Z"
}
```

**Erros:**
- `400` — attachmentId ausente ou inválido
- `404` — Attachment não encontrado
- `409` — já existe Document vinculado a este Attachment

### 9.2 GET /api/documents/{id}

**Response (200):** `DocumentResponse`

**Erros:**
- `404` — documento não encontrado

### 9.3 GET /api/documents/{id}/status

**Response (200):**
```json
{
  "id": 1,
  "status": "INDEXED",
  "errorMessage": null,
  "updatedAt": "2026-06-27T10:05:00Z"
}
```

**Erros:**
- `404` — documento não encontrado

### 9.4 DELETE /api/documents/{id}

**Responsabilidade:** Remover Document e chunks. Attachment não é removido.

**Response (204):** sem corpo

**Erros:**
- `404` — documento não encontrado

### 9.5 POST /api/documents/{id}/reprocess

**Response (200):**
```json
{
  "id": 1,
  "status": "RECEIVED",
  "message": "Reprocessamento solicitado."
}
```

**Erros:**
- `404` — documento não encontrado
- `409` — documento está em PROCESSING
- `500` — falha não recuperável ao alterar status

### 9.6 Resumo de status HTTP

| Situação | Código |
|---|---|
| Sucesso (POST) | `201` |
| Sucesso (GET) | `200` |
| Sucesso (DELETE) | `204` |
| Parâmetro ausente ou inválido | `400` |
| Recurso não encontrado | `404` |
| Conflito de estado | `409` |
| Tamanho de upload excedido | `413` (existente) |
| Falha não recuperável | `500` |

Exemplos de `ProblemDetail`:

```json
// 404 - Attachment nao encontrado
{
  "title": "Arquivo nao encontrado",
  "status": 404,
  "detail": "Nao foi encontrado um arquivo com o ID 999."
}

// 404 - Documento nao encontrado
{
  "title": "Documento nao encontrado",
  "status": 404,
  "detail": "Nao foi encontrado um documento com o ID 999."
}

// 409 - Conflito
{
  "title": "Conflito de estado",
  "status": 409,
  "detail": "O Attachment 1 ja possui um documento de indexacao. Use reprocessamento se necessario."
}

// 500 - Falha interna
{
  "title": "Erro interno",
  "status": 500,
  "detail": "Ocorreu um erro inesperado. Tente novamente mais tarde."
}
```

---

## 10. Contrato com o frontend

### 10.1 DocumentResponse

```json
{
  "id": 1,
  "attachmentId": 1,
  "sessionId": 3,
  "fileName": "diario.pdf",
  "contentType": "application/pdf",
  "size": 102400,
  "status": "INDEXED",
  "createdAt": "2026-06-27T10:00:00Z",
  "updatedAt": "2026-06-27T10:05:00Z"
}
```

### 10.2 DocumentStatusResponse

```json
{
  "id": 1,
  "status": "INDEXED",
  "errorMessage": null,
  "updatedAt": "2026-06-27T10:05:00Z"
}
```

### 10.3 RagSource

```json
{
  "documentId": 1,
  "attachmentId": 1,
  "fileName": "diario.pdf",
  "content": "Trecho relevante extraido do documento...",
  "chunkIndex": 3,
  "score": 0.87
}
```

### 10.4 ChatResponse com sources

O `ChatResponse` existente (Parte 1) recebe uma evolução aditiva e retrocompatível. O novo campo `sources` é **sempre retornado**; quando não houver fontes, seu valor é `[]` (nunca null):

```json
{
  "userMessage": { "id": 10, "content": "...", "role": "USER", "timestamp": "..." },
  "assistantMessage": { "id": 11, "content": "...", "role": "ASSISTANT", "timestamp": "..." },
  "sources": [
    {
      "documentId": 1,
      "attachmentId": 1,
      "fileName": "diario.pdf",
      "content": "Trecho relevante...",
      "chunkIndex": 3,
      "score": 0.87
    }
  ]
}
```

---

## 11. Contrato do webhook n8n

**URL:** configurável via `n8n.webhook.url`.

**Momento do disparo:** **Após o commit** da transação que marcou o documento como `INDEXED`. Implementado via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.

**Obrigatoriedade:** O disparo é obrigatório após toda indexação bem-sucedida. A falha da chamada HTTP não altera o status `INDEXED` nem remove chunks — apenas gera log e registro para futura retentativa.

**Payload:**
```json
{
  "event": "document.indexed",
  "documentId": 1,
  "attachmentId": 1,
  "fileName": "diario.pdf",
  "contentType": "application/pdf",
  "size": 102400,
  "status": "INDEXED",
  "sessionId": 3,
  "chunkCount": 12,
  "timestamp": "2026-06-27T10:05:00Z"
}
```

**Campos obrigatórios no payload:** `event`, `documentId`, `status`, `timestamp`.

**Tratamento de falha:**
- Timeout: configurável (ex.: 5000ms — valor exemplificativo).
- Erro HTTP (timeout, 4xx, 5xx): log WARN com detalhes.
- Não altera `INDEXED`.
- Não remove chunks.
- n8n pode consultar `GET /api/documents/{id}` ou `GET /api/documents/{id}/status` para reconciliação.

**Nenhuma regra de negócio do MindJourney é implementada no n8n.** O backend conhece apenas a URL do webhook e o contrato do payload.

---

## 12. Proposta da migration V3

### Tabela `documents`

| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PRIMARY KEY |
| `attachment_id` | `BIGINT` | NOT NULL, UNIQUE, FK -> `attachments(id)` ON DELETE CASCADE |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT 'RECEIVED', CHECK (status IN ('RECEIVED','PROCESSING','INDEXED','FAILED')) |
| `error_message` | `TEXT` | NULLABLE |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL |

### Tabela `document_chunks`

| Coluna | Tipo | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PRIMARY KEY |
| `document_id` | `BIGINT` | NOT NULL, FK -> `documents(id)` ON DELETE CASCADE |
| `content` | `TEXT` | NOT NULL |
| `chunk_index` | `INTEGER` | NOT NULL |
| `embedding` | `vector(D)` | NOT NULL (Dimensão D: **decisão pendente**) |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | NOT NULL |

### Constraints adicionais

```sql
ALTER TABLE document_chunks ADD CONSTRAINT uk_document_chunk_index
    UNIQUE (document_id, chunk_index);

CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_attachment_id ON documents(attachment_id);
CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);
```

**Índice vetorial:** NÃO incluído na V3. A estratégia deve ser definida após conhecer o volume de dados. Pode ser adicionado em migration posterior V4.

### Compatibilidade

- V1: `CREATE EXTENSION IF NOT EXISTS vector;`
- V2: `journal_sessions`, `messages`, `attachments`
- V3: adiciona `documents` e `document_chunks`. Nenhuma tabela existente é alterada.

Rollback:
```sql
DROP TABLE IF EXISTS document_chunks;
DROP TABLE IF EXISTS documents;
```

---

## 13. Configurações externas

Nenhum valor hardcoded. Parâmetros em `application-postgres.yml`. Valores abaixo são **exemplificativos** — a equipe deve definir os definitivos:

```yaml
rag:
  chunk:
    max-size: ${RAG_CHUNK_MAX_SIZE:1000}          # EXEMPLO -- decisao pendente
    overlap: ${RAG_CHUNK_OVERLAP:200}              # EXEMPLO -- decisao pendente
  retrieval:
    top-k: ${RAG_TOP_K:5}                          # EXEMPLO -- decisao pendente
    min-similarity: ${RAG_MIN_SIMILARITY:0.75}     # EXEMPLO -- decisao pendente
  embedding:
    dimension: ${RAG_EMBEDDING_DIMENSION:768}      # EXEMPLO -- decisao pendente
  webhook:
    url: ${N8N_WEBHOOK_URL:}
    timeout: ${N8N_WEBHOOK_TIMEOUT:5000}           # EXEMPLO -- decisao pendente
```

**Validação em startup:** Um componente com `@PostConstruct` valida que `rag.embedding.dimension` é positivo e, se possível, compatível com a coluna `vector(D)` já existente no banco.

---

## 14. Estratégia de testes

### 14.1 Unitários
- `DocumentParserTest`: TXT, PDF mock, bytes vazios -> exceção.
- `TextChunkerTest`: tamanhos, overlap, texto curto.
- `DocumentStatusTest`: transições válidas e inválidas.

### 14.2 Services (mocks)
- `DocumentIngestionServiceTest`: mock de `IngestionTransactionService`, `DocumentParser`, `TextChunker`, `EmbeddingService`, `AttachmentRepository`. Verifica fluxo completo: markAsProcessing -> preparação -> replaceChunksAndMarkIndexed -> evento. Verifica falha -> markAsFailed.
- `RagServiceTest`: mock de `EmbeddingService` e `DocumentChunkRepository`. Verifica `RagContext` com contexto formatado e `sources` com score. Verifica que `sessionId` é repassado ao repository.

### 14.3 Repositories (Testcontainers + pgvector)
- `DocumentRepositoryTest`: CRUD, existsByAttachmentId.
- `DocumentChunkRepositoryTest`: persistência com vector, busca com `<=>`, score, cascade, UNIQUE constraint.

### 14.4 Testes de isolamento por sessão
- `DocumentChunkRepositoryTest`: inserir chunks de duas sessões diferentes; verificar que `findSimilarChunks(sessionIdA, ...)` não retorna chunks da sessão B.

### 14.5 Migrations (Flyway + Testcontainers)
- V1->V2->V3 executam sem erro.
- Verificar constraints e tipos.

### 14.6 Contratos HTTP (`@WebMvcTest`)
- `DocumentControllerTest`: POST 201/400/404/409, GET 200/404, GET status 200/404, DELETE 204/404, reprocess 200/404/409.

### 14.7 Testes do evento pós-commit
- `DocumentIndexedEventListenerTest`: mock do `N8nWebhookClient`; publicar `DocumentIndexedEvent`; verificar que `notify()` foi chamado.
- Teste de rollback: simular falha em `replaceChunksAndMarkIndexed`; verificar que `notify()` **não** foi chamado.

### 14.8 Mocks
- `MemoryEmbeddingService`: vetores determinísticos para testes.
- `N8nWebhookClient` mock: verifica chamada sem enviar HTTP real.

---

## 15. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| Dependência de parsing PDF precisa ser adicionada | PDF não processável | Adicionar lib ao `pom.xml` na implementação |
| Dimensão do embedding incompatível com coluna pgvector | Erro em runtime | Validação em startup; migration V3 bloqueada até definição |
| Representação Java do vector sem suporte Hibernate | Erro de mapeamento JPA | Testar com pgvector JDBC driver antes da V3 |
| n8n fora do ar | Apenas log | Webhook não bloqueia ingestão |
| H2 não suporta pgvector | Testes com H2 inviáveis | Testes de repository usam Testcontainers; profile H2 mantido para dev sem RAG |
| Documento preso em PROCESSING após queda | Inconsistência | `markAsFailed` imediato captura exceção; job de fallback opcional |
| Performance sem índice vetorial | Degradação com muitos chunks | Adiar decisão; começar sem índice, adicionar em V4 |

---

## 16. Decisões pendentes

### Bloqueiam a migration V3

| Decisão | Impacto |
|---|---|
| **Provedor e modelo de embedding** | Define qual serviço externo será chamado e a dimensão do vetor |
| **Dimensão do vetor (D)** | Impacta `vector(D)` na DDL e a configuração `rag.embedding.dimension` |
| **Representação Java do vetor + integração Hibernate/pgvector** | Necessário verificar se `float[]` com `columnDefinition = "vector(D)"` funciona no Hibernate 6 / Spring Boot 3.4, ou se exige tipo customizado (ex.: `PGvector` do driver pgvector-jdbc) |

**Nota:** A relação Document -> Attachment (obrigatória e exclusiva) está **definitivamente aprovada** nesta especificação e não bloqueia a V3.

### Não bloqueiam a V3, mas precisam de decisão

| Decisão | Observação |
|---|---|
| Estratégia de índice vetorial | Pode ser adicionado em V4 após benchmark |
| Tamanho do chunk, overlap, top-k, min-similarity | Parâmetros configuráveis; ajustáveis a qualquer momento |
| Timeout do webhook | Default exemplificativo; ajustável |
| Biblioteca de parsing PDF | `pom.xml` atualizado na implementação |
| Estratégia de execução assíncrona | `@Async` vs. fila |

---

## 17. Arquivos futuros previstos

### Entidades
- `src/main/java/com/mindjournal/entity/Document.java`
- `src/main/java/com/mindjournal/entity/DocumentStatus.java`
- `src/main/java/com/mindjournal/entity/DocumentChunk.java`

### DTOs
- `src/main/java/com/mindjournal/dto/DocumentResponse.java`
- `src/main/java/com/mindjournal/dto/DocumentStatusResponse.java`
- `src/main/java/com/mindjournal/dto/CreateDocumentRequest.java`
- `src/main/java/com/mindjournal/dto/RagSource.java`
- `src/main/java/com/mindjournal/dto/RagContext.java`

### Repositories
- `src/main/java/com/mindjournal/repository/DocumentRepository.java`
- `src/main/java/com/mindjournal/repository/DocumentChunkRepository.java`

### Services
- `src/main/java/com/mindjournal/service/rag/DocumentIngestionService.java`
- `src/main/java/com/mindjournal/service/rag/IngestionTransactionService.java`
- `src/main/java/com/mindjournal/service/rag/RagService.java`
- `src/main/java/com/mindjournal/service/parsing/DocumentParser.java`
- `src/main/java/com/mindjournal/service/chunking/TextChunker.java`
- `src/main/java/com/mindjournal/service/embedding/EmbeddingService.java` (interface)
- `src/main/java/com/mindjournal/service/embedding/MemoryEmbeddingService.java`
- `src/main/java/com/mindjournal/service/webhook/N8nWebhookClient.java`
- `src/main/java/com/mindjournal/service/DocumentService.java`

### Controllers
- `src/main/java/com/mindjournal/controller/DocumentController.java`

### Exceptions
- `src/main/java/com/mindjournal/exception/DocumentNotFoundException.java`
- `src/main/java/com/mindjournal/exception/EmptyDocumentException.java`
- `src/main/java/com/mindjournal/exception/ParsingException.java`
- `src/main/java/com/mindjournal/exception/EmbeddingException.java`
- `src/main/java/com/mindjournal/exception/DocumentStateConflictException.java`

### Existentes com adições retrocompatíveis
- `ChatResponse.java` — campo `List<RagSource> sources` (sempre presente, default `[]`)
- `GlobalExceptionHandler.java` — handlers para novas exceções
- `application-postgres.yml` — seção `rag.*`
- `.env.example` — variáveis RAG (opcional)

### Migration
- `src/main/resources/db/migration/V3__create_rag_tables.sql`

### Documentação
- `docs/parte-2/rag-domain-spec.md` (este documento)
