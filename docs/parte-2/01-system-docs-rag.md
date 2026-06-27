# System Docs — Pipeline RAG do MindJourney IA

## 1. Princípios
- Controllers tratam HTTP e delegam.
- Parsing, chunking, embedding e persistência permanecem separados.
- `EmbeddingService` recebe texto e retorna vetor, sem conhecer documentos.
- `RagService` apenas orquestra embedding da pergunta, recuperação e formatação do contexto.
- O n8n é externo ao domínio.

## 2. Modelo de domínio

### Document
- `id`
- `originalFilename`
- `contentType`
- `size`
- `status`
- `createdAt`
- `updatedAt`
- `errorMessage`

### DocumentStatus
- `RECEIVED`
- `PROCESSING`
- `INDEXED`
- `FAILED`

### DocumentChunk
- `id`
- `documentId`
- `content`
- `chunkIndex`
- `embedding`
- `createdAt`

## 3. Contratos públicos

### DocumentParser
```java
boolean supports(String contentType, String filename);
String parse(byte[] content, String contentType, String filename);
```

### TextChunker
```java
List<String> split(String text);
```

### EmbeddingService
```java
List<Float> embed(String text);
List<List<Float>> embedAll(List<String> texts);
```

### DocumentIngestionService
```java
DocumentResponse ingest(Long documentId);
```

### RagService
```java
RagContext retrieve(String question);
```

### N8nWebhookClient
```java
void notifyDocumentIndexed(DocumentIndexedWebhook payload);
```

## 4. Sequência da ingestão

1. Controller recebe upload.
2. Serviço de documento valida arquivo e persiste o registro como `RECEIVED`.
3. `DocumentIngestionService` altera o estado para `PROCESSING`.
4. Seleciona um `DocumentParser`.
5. Extrai texto puro.
6. `TextChunker` divide o texto.
7. `EmbeddingService` gera os vetores.
8. `DocumentChunkRepository` persiste chunks e embeddings.
9. Documento passa para `INDEXED`.
10. `N8nWebhookClient` envia o evento.
11. Em falha, documento passa para `FAILED` e guarda mensagem sanitizada.

## 5. Retrieval

1. Receber pergunta.
2. Gerar embedding da pergunta.
3. Consultar os chunks por similaridade.
4. Aplicar `topK` e `minSimilarity` vindos de configuração.
5. Formatar contexto.
6. Retornar contexto e fontes.
7. O serviço de chat envia pergunta e contexto ao gerador de resposta.

## 6. Fonte retornada ao frontend

```json
{
  "documentId": 12,
  "documentName": "diario-junho.pdf",
  "chunkId": 84,
  "chunkIndex": 3,
  "content": "Trecho utilizado para fundamentar a resposta.",
  "similarity": 0.86
}
```

## 7. Resposta do chat

```json
{
  "sessionId": 1,
  "userMessage": {
    "id": 20,
    "role": "USER",
    "content": "O que escrevi sobre meu objetivo profissional?"
  },
  "assistantMessage": {
    "id": 21,
    "role": "ASSISTANT",
    "content": "Você registrou que deseja..."
  },
  "sources": []
}
```

## 8. Status do documento

`GET /api/documents/{id}/status`

```json
{
  "documentId": 12,
  "status": "INDEXED",
  "updatedAt": "2026-06-27T16:00:00Z",
  "errorMessage": null
}
```

## 9. Webhook n8n

```json
{
  "event": "DOCUMENT_INDEXED",
  "documentId": 12,
  "filename": "diario-junho.pdf",
  "status": "INDEXED",
  "chunkCount": 18,
  "indexedAt": "2026-06-27T16:00:00Z"
}
```

## 10. Configurações

```yaml
rag:
  top-k: ${RAG_TOP_K:5}
  min-similarity: ${RAG_MIN_SIMILARITY:0.70}
  chunk-size: ${RAG_CHUNK_SIZE:1000}
  chunk-overlap: ${RAG_CHUNK_OVERLAP:150}

n8n:
  webhook-url: ${N8N_WEBHOOK_URL:}
```

## 11. Erros HTTP
- 400: arquivo inválido, pergunta vazia ou formato não suportado.
- 404: documento ou sessão inexistente.
- 409: tentativa de reindexar documento em processamento ou já indexado.
- 500: falha não recuperável.
