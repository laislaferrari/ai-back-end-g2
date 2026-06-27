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
  "userMessage": { "id": 10, "content": "...", "role": "USER", "timestamp": "..." },
  "assistantMessage": { "id": 11, "content": "...", "role": "ASSISTANT", "timestamp": "..." }
}
```

### Upload — `POST /api/upload`
**Request:** `multipart/form-data` com campos `file` + `sessionId`.
**Response (201):**
```json
{
  "id": 1, "sessionId": 1, "filename": "diario.txt", "type": "TXT", "size": 1024, "uploadDate": "..."
}
```

---

## 3. Mudanças mínimas necessárias (visão geral)

### Fase 1 — Infraestrutura (esta execução)
- Adicionar dependências PostgreSQL, Flyway e test ao `pom.xml`
- Criar `application-postgres.yml` com datasource PostgreSQL + env vars
- Criar `.env.example`
- Criar `docker-compose.yml` com pgvector
- Criar migration `V1__enable_pgvector.sql`
- Atualizar `.gitignore`
- Atualizar `README.md`

### Fases futuras (não implementar agora)
- Entidades `Document`, `DocumentChunk`, `DocumentStatus`
- Serviços de parsing, chunking, embedding, ingestão, retrieval
- Controller de documentos
- Webhook n8n

---

## 4. Ordem de implementação completa

```
Fase 0 — Plano e documentação (este arquivo)
Fase 1 — Infraestrutura PostgreSQL + pgvector ← ESTAMOS AQUI
Fase 2 — Domínio Document + DocumentChunk
Fase 3 — Parsing e Chunking
Fase 4 — Embedding Service
Fase 5 — Pipeline de ingestão
Fase 6 — Retrieval e fontes no chat
Fase 7 — n8n webhook
Fase 8 — Testes
Fase 9 — Documentação final
```

---

## 5. Riscos

| Risco | Mitigação |
|---|---|
| Mudar `ChatResponse` | `sources` opcional (`[]`) |
| H2 → PostgreSQL migração | Perfil H2 mantido como default |
| Dependência externa (OpenAI) | `AiServiceException` → 502 |
| n8n fora do ar | Fallback silencioso |

---

## 6. Compatibilidade com frontend

Nenhum endpoint existente é alterado nesta fase. O perfil H2 continua sendo o default.

---

## 7. Proposta de testes

Fase atual não implementa lógica nova. Testes validam que:
- Compilação passa com as novas dependências
- Perfil H2 continua funcional (teste existente)
