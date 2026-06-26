# Plano de Implementação — Back-end MindJournal AI

## Fase 1: Bootstrap e Configuração Inicial

### Objetivo
Inicializar o projeto Spring Boot com as dependências corretas e configurar o ambiente de desenvolvimento.

### Dependências
- Nenhuma (fase inicial)

### Arquivos e pacotes envolvidos
- `pom.xml` (raiz do projeto)
- `src/main/resources/application.yml`
- `src/main/java/com/mindjournal/MindJournalApplication.java`

### Tarefas de implementação
1. Criar `pom.xml` com:
   - `groupId`: `com.mindjournal`
   - Java 17
   - Spring Boot 3 (spring-boot-starter-parent)
   - Dependências: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `h2`, `spring-boot-starter-validation`, `spring-boot-devtools` (opcional)
   - Plugins: `spring-boot-maven-plugin`
2. Criar `application.yml` com:
   - `server.port: 8080`
   - `spring.datasource.url: jdbc:h2:file:./data/mindjournal`
   - `spring.datasource.driver-class-name: org.h2.Driver`
   - `spring.datasource.username: sa`
   - `spring.datasource.password:`
   - `spring.jpa.database-platform: org.hibernate.dialect.H2Dialect`
   - `spring.jpa.hibernate.ddl-auto: update`
   - `spring.jpa.show-sql: false`
   - `spring.h2.console.enabled: true`
   - `spring.servlet.multipart.max-file-size: 10MB`
   - `spring.servlet.multipart.max-request-size: 11MB`
3. Criar classe principal `MindJournalApplication` com `@SpringBootApplication`

### Contratos da API afetados
- Nenhum ainda

### Validações necessárias
- Verificar se a aplicação sobe sem erros
- Verificar se o console H2 está acessível em `/h2-console`

### Testes manuais recomendados
- Executar `mvn spring-boot:run` e confirmar que a aplicação inicia na porta 8080
- Acessar `http://localhost:8080/h2-console` e conectar com JDBC URL `jdbc:h2:file:./data/mindjournal`

### Critérios de aceite
- [ ] `mvn clean compile` executa sem erros
- [ ] Aplicação inicia na porta 8080
- [ ] H2 console funcional
- [ ] Arquivo `data/mindjournal.mv.db` é criado no diretório `data/`

### Definition of Done
- Commit com `pom.xml`, `application.yml` e classe principal

---

## Fase 2: Entidades e Enums

### Objetivo
Criar as entidades JPA e os enums que representam o domínio.

### Dependências
- Fase 1 (bootstrap)

### Arquivos e pacotes envolvidos
- `src/main/java/com/mindjournal/model/Session.java`
- `src/main/java/com/mindjournal/model/Message.java`
- `src/main/java/com/mindjournal/model/Attachment.java`
- `src/main/java/com/mindjournal/model/Sender.java`
- `src/main/java/com/mindjournal/model/AttachmentType.java`

### Tarefas de implementação
1. Criar enum `Sender` com valores `USER` e `ASSISTANT`
2. Criar enum `AttachmentType` com valores `TXT` e `PDF`
3. Criar entidade `Session`:
   - `@Entity`, `@Table(name = "sessions")`
   - Campos: `id` (Long, `@Id`, `@GeneratedValue`), `title` (String, nullable), `createdAt` (Instant), `updatedAt` (Instant)
   - `@PrePersist` e `@PreUpdate` para definir `createdAt`/`updatedAt`
4. Criar entidade `Message`:
   - `@Entity`, `@Table(name = "messages")`
   - Campos: `id` (Long), `session` (`@ManyToOne`, `@JoinColumn(name = "session_id")`), `sender` (`@Enumerated(STRING)`), `content` (String, `@Column(columnDefinition = "TEXT")`), `timestamp` (Instant)
5. Criar entidade `Attachment`:
   - `@Entity`, `@Table(name = "attachments")`
   - Campos: `id` (Long), `session` (`@ManyToOne`), `filename` (String), `type` (`@Enumerated(STRING)`), `size` (Long), `filePath` (String), `uploadDate` (Instant)

### Contratos da API afetados
- Nenhum ainda

### Validações necessárias
- Verificar que as tabelas são criadas automaticamente pelo `ddl-auto: update`

### Testes manuais recomendados
- Subir a aplicação e verificar no console H2 se as tabelas `sessions`, `messages` e `attachments` foram criadas com as colunas corretas

### Critérios de aceite
- [ ] Entidades compilam sem erros
- [ ] Tabelas criadas automaticamente com colunas corretas
- [ ] Relacionamentos `@ManyToOne` configurados

### Definition of Done
- Commit com entidades e enums

---

## Fase 3: DTOs e Configuração de Erro

### Objetivo
Criar os DTOs dos contratos da API e o tratamento global de erros.

### Dependências
- Fase 2 (entidades)

### Arquivos e pacotes envolvidos
- `src/main/java/com/mindjournal/dto/` (todos os DTOs)
- `src/main/java/com/mindjournal/exception/GlobalExceptionHandler.java`
- `src/main/java/com/mindjournal/exception/ProblemDetailFactory.java` (opcional)

### Tarefas de implementação
1. Criar DTOs como `record` do Java 17:
   - `ChatRequest(Long sessionId, String content)` com validação `@NotNull sessionId`, `@NotBlank content`
   - `ChatResponse(MessageDTO userMessage, MessageDTO assistantMessage)`
    - `CreateSessionRequest(String title)` — nullable, service define `"Nova sessão"` como padrão
   - `SessionDTO(Long id, String title, Instant createdAt, Instant updatedAt)`
   - `MessageDTO(Long id, Long sessionId, Sender sender, String content, Instant timestamp)`
   - `AttachmentDTO(Long id, Long sessionId, String filename, AttachmentType type, Long size, Instant uploadDate)`
   - `HealthDTO(String status, Instant timestamp)`
2. Criar `GlobalExceptionHandler` com `@RestControllerAdvice`:
   - `handleMethodArgumentNotValid` → 400 `ProblemDetail`
   - `handleNoSuchElementException` / custom exception → 404 `ProblemDetail`
   - `handleMaxUploadSizeExceededException` → 413 `ProblemDetail`
   - `handleGenericException` → 500 `ProblemDetail`
3. Configurar `ProblemDetail` com campos `status`, `title`, `detail`

### Contratos da API afetados
- Todos os endpoints (DTOs de entrada/saída)

### Validações necessárias
- `@NotNull` e `@NotBlank` nos campos obrigatórios de DTOs
- Conteúdo da mensagem não pode ser vazio

### Testes manuais recomendados
- Nenhum (testado via controllers nas fases seguintes)

### Critérios de aceite
- [ ] Todos os DTOs compilam como `record`
- [ ] Validações com `jakarta.validation` configuradas
- [ ] `ProblemDetail` retorna campos `status`, `title`, `detail` corretamente

### Definition of Done
- Commit com DTOs e exception handler

---

## Fase 4: Repositories

### Objetivo
Criar os repositórios JPA com os métodos de consulta necessários.

### Dependências
- Fase 2 (entidades)

### Arquivos e pacotes envolvidos
- `src/main/java/com/mindjournal/repository/SessionRepository.java`
- `src/main/java/com/mindjournal/repository/MessageRepository.java`
- `src/main/java/com/mindjournal/repository/AttachmentRepository.java`

### Tarefas de implementação
1. Criar `SessionRepository`:
   - `extends JpaRepository<Session, Long>`
   - `findAllByOrderByUpdatedAtDesc()` — lista sessões ordenadas por `updatedAt` decrescente
2. Criar `MessageRepository`:
   - `extends JpaRepository<Message, Long>`
   - `findBySessionIdOrderByTimestampAsc(Long sessionId)` — histórico ordenado por `timestamp` crescente
3. Criar `AttachmentRepository`:
   - `extends JpaRepository<Attachment, Long>`
   - `findBySessionId(Long sessionId)` — anexos de uma sessão (opcional Etapa 1)

### Contratos da API afetados
- Nenhum diretamente (usado pelos services)

### Validações necessárias
- Confirmar que as queries geradas pelo Spring Data JPA estão corretas

### Testes manuais recomendados
- Nenhum (testado via services nas fases seguintes)

### Critérios de aceite
- [ ] Repositórios compilam
- [ ] Métodos de ordenação implementados conforme especificação

### Definition of Done
- Commit com repositories

---

## Fase 5: Services — Sessões e Mensagens

### Objetivo
Implementar a lógica de negócio para sessões e mensagens.

### Dependências
- Fase 3 (DTOs)
- Fase 4 (repositories)

### Arquivos e pacotes envolvidos
- `src/main/java/com/mindjournal/service/SessionService.java`
- `src/main/java/com/mindjournal/service/MessageService.java`

### Tarefas de implementação
1. Criar `SessionService` com três métodos de acesso à sessão:
    - `getSession(Long id)`: retorna `SessionDTO` — usado pelos Controllers para responder à API
    - `requireSession(Long id)`: retorna a entidade `Session` — usado internamente por `MessageService`, `ChatService` e `AttachmentService`; lança exceção se não encontrada; **nunca chamado por Controllers**
    - `touchSession(Long id)`: atualiza `updatedAt` da sessão para `Instant.now()` e persiste
    - `listSessions()` → retorna `List<SessionDTO>` ordenado por `updatedAt` desc
    - `createSession(CreateSessionRequest)` → se `title` for nulo, vazio ou apenas espaços, define como `"Nova sessão"`; cria `Session`, persiste, retorna `SessionDTO`
2. Criar `MessageService`:
   - `getMessagesBySessionId(Long sessionId)` → valida existência da sessão via `SessionService`, retorna `List<MessageDTO>` ordenado por `timestamp` asc
   - `createMessage(Session session, Sender sender, String content)` → cria e persiste `Message`, retorna `MessageDTO`
3. Ambos os services validam existência da sessão (lançam `NoSuchElementException` ou custom exception para `GlobalExceptionHandler` tratar como 404)

### Contratos da API afetados
- `POST /api/sessions`
- `GET /api/sessions`
- `GET /api/sessions/{id}`
- `GET /api/sessions/{id}/messages`

### Validações necessárias
- Sessão inexistente → exceção → 404
- Lista vazia retorna array vazio, não erro

### Testes manuais recomendados
- Nenhum (testado via controllers na Fase 7)

### Critérios de aceite
- [ ] `SessionService` retorna sessões ordenadas
- [ ] `MessageService` valida existência da sessão
- [ ] Mensagens retornam ordenadas por timestamp asc

### Definition of Done
- Commit com `SessionService` e `MessageService`

---

## Fase 6: Service — Resposta Simulada e Chat

### Objetivo
Implementar o serviço de chat com a resposta simulada do assistente.

### Dependências
- Fase 5 (SessionService, MessageService)

### Arquivos e pacotes envolvidos
- `src/main/java/com/mindjournal/service/ChatService.java`
- `src/main/java/com/mindjournal/service/AiResponseGenerator.java`
- `src/main/java/com/mindjournal/service/MockAiResponseGenerator.java`

### Tarefas de implementação
1. Criar interface `AiResponseGenerator` com método `generateResponse(Long sessionId, String userMessage): String`
2. Criar `MockAiResponseGenerator` implementando a interface:
   - Lógica simples baseada em palavras-chave para gerar resposta simulada
   - Ex: se "triste" → resposta de acolhimento; se "feliz" → resposta de celebração
3. Criar `ChatService`:
    - `sendMessage(ChatRequest)` valida sessão, persiste mensagem do usuário, invoca `AiResponseGenerator`, persiste resposta, chama `sessionService.touchSession(sessionId)`, retorna `ChatResponse` com ambas as mensagens
4. `MockAiResponseGenerator` anotado com `@Service` e `@Primary` (pode ser substituído via `@Profile` no futuro)

### Contratos da API afetados
- `POST /api/chat` (lógica de negócio)

### Validações necessárias
- Sessão deve existir
- Conteúdo não pode ser vazio

### Testes manuais recomendados
- Nenhum (testado via controller na Fase 7)

### Critérios de aceite
- [ ] `MockAiResponseGenerator` retorna respostas baseadas em palavras-chave
- [ ] `ChatService` persiste ambas as mensagens
- [ ] `ChatResponse` contém `userMessage` e `assistantMessage`

### Definition of Done
- Commit com `ChatService`, `AiResponseGenerator` e `MockAiResponseGenerator`

---

## Fase 7: Controllers — Health, Sessões e Chat

### Objetivo
Expor os endpoints de health check, sessões e chat.

### Dependências
- Fase 5 (SessionService)
- Fase 6 (ChatService)

### Arquivos e pacotes envolvidos
- `src/main/java/com/mindjournal/controller/HealthController.java`
- `src/main/java/com/mindjournal/controller/SessionController.java`
- `src/main/java/com/mindjournal/controller/ChatController.java`
- `src/main/java/com/mindjournal/controller/MessageController.java`

### Tarefas de implementação
1. Criar `HealthController`:
   - `GET /api/health` → retorna `HealthDTO` com status `"UP"` e timestamp atual
2. Criar `SessionController`:
   - `POST /api/sessions` → recebe `@RequestBody CreateSessionRequest`, delega para `SessionService`, retorna 201
   - `GET /api/sessions` → delega para `SessionService`, retorna 200
   - `GET /api/sessions/{id}` → delega para `SessionService`, retorna 200 ou 404
3. Criar `ChatController`:
   - `POST /api/chat` → recebe `@Valid @RequestBody ChatRequest`, delega para `ChatService`, retorna 201
4. Criar `MessageController`:
   - `GET /api/sessions/{id}/messages` → delega para `MessageService`, retorna 200 ou 404
5. Nenhum controller contém regra de negócio — apenas delega para services
6. Validar que `@RequestMapping("/api")` ou prefixo está configurado

### Contratos da API afetados
- `GET /api/health`
- `POST /api/sessions`
- `GET /api/sessions`
- `GET /api/sessions/{id}`
- `POST /api/chat`
- `GET /api/sessions/{id}/messages`

### Validações necessárias
- Payloads com `@Valid` nos controllers
- Caminho da annotation `@RequestMapping("/api")` na classe ou application.yml

### Testes manuais recomendados
- `curl http://localhost:8080/api/health` → 200 com `{"status":"UP","timestamp":"..."}`
- `curl -X POST http://localhost:8080/api/sessions -H "Content-Type: application/json" -d '{"title":"Minha sessão"}'` → 201
- `curl http://localhost:8080/api/sessions` → 200 com lista
- `curl http://localhost:8080/api/sessions/1` → 200 ou 404
- `curl -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d '{"sessionId":1,"content":"Hoje foi um dia difícil"}'` → 201 com `userMessage` e `assistantMessage`
- `curl http://localhost:8080/api/sessions/1/messages` → 200 com histórico

### Critérios de aceite
- [ ] `GET /api/health` funcional
- [ ] Operações de criação, listagem e busca de sessões funcionais
- [ ] Chat funcional com resposta simulada
- [ ] Histórico de mensagens funcional
- [ ] Nenhum controller contém regra de negócio

### Definition of Done
- Commit com controllers (health, sessão, chat, mensagem)

---

## Fase 8: Service — Upload e Anexos

### Objetivo
Implementar a lógica de upload de arquivos com validações.

### Dependências
- Fase 5 (SessionService)
- Fase 4 (AttachmentRepository)

### Arquivos e pacotes envolvidos
- `src/main/java/com/mindjournal/service/AttachmentService.java`
- `src/main/java/com/mindjournal/config/StorageConfig.java` (opcional, para diretório de uploads)

### Tarefas de implementação
1. Criar `AttachmentService`:
   - Método `upload(String originalFilename, String mimeType, long size, byte[] content, Long sessionId)`:
     - Valida existência da sessão via `SessionService`
     - Valida extensão (.txt, .pdf)
     - Valida tipo MIME (text/plain, application/pdf)
     - Valida tamanho (<= 10 MB)
     - Salva arquivo no sistema de arquivos (diretório configurável, ex: `./uploads/`)
     - Persiste metadados via `AttachmentRepository`
      - Chama `sessionService.touchSession(sessionId)` após persistir
      - Retorna `AttachmentDTO`
2. **Não recebe `MultipartFile`** — recebe parâmetros primitivos independentes de HTTP

### Contratos da API afetados
- `POST /api/upload` (lógica de negócio)
- `GET /api/sessions/{id}/attachments` (opcional Etapa 1)

### Validações necessárias
- Sessão deve existir
- Extensão: apenas `.txt` e `.pdf`
- Tipo MIME: `text/plain` e `application/pdf`
- Tamanho: máximo 10 MB

### Testes manuais recomendados
- Nenhum (testado via controller na Fase 9)

### Critérios de aceite
- [ ] `AttachmentService` valida extensão, tipo MIME e tamanho
- [ ] `AttachmentService` não recebe `MultipartFile` diretamente
- [ ] Arquivo salvo em disco e metadados persistidos

### Definition of Done
- Commit com `AttachmentService`

---

## Fase 9: Controller — Upload

### Objetivo
Expor o endpoint de upload, realizando a conversão de `MultipartFile` para parâmetros independentes de HTTP.

### Dependências
- Fase 8 (AttachmentService)
- Fase 3 (DTOs)

### Arquivos e pacotes envolvidos
- `src/main/java/com/mindjournal/controller/AttachmentController.java`
- `src/main/java/com/mindjournal/dto/UploadRequest.java` (opcional, objeto intermediário)

### Tarefas de implementação
1. Criar `AttachmentController`:
   - `POST /api/upload`:
     - Recebe `@RequestParam("file") MultipartFile file` e `@RequestParam("sessionId") Long sessionId`
     - Extrai `originalFilename`, `contentType` (MIME), `size`, `bytes` do `MultipartFile`
     - Cria objeto intermediário (ex: `UploadRequest`) ou passa parâmetros diretamente
     - Delega para `AttachmentService`
     - Retorna `AttachmentDTO` com status 201
   - `GET /api/sessions/{id}/attachments` (opcional Etapa 1):
     - Delega para `AttachmentService`, retorna lista de `AttachmentDTO`
2. Controller apenas converte e delega — sem regras de negócio

### Contratos da API afetados
- `POST /api/upload`
- `GET /api/sessions/{id}/attachments` (opcional)

### Validações necessárias
- `sessionId` obrigatório
- Tipo de arquivo validado pelo service
- Tamanho validado pelo service (e pelo Spring com `spring.servlet.multipart.max-file-size`)

### Testes manuais recomendados
- Upload TXT: `curl -F "file=@teste.txt" -F "sessionId=1" http://localhost:8080/api/upload` → 201
- Upload PDF: `curl -F "file=@teste.pdf" -F "sessionId=1" http://localhost:8080/api/upload` → 201
- Upload sem sessionId: → 400
- Upload tipo inválido: → 400 com `ProblemDetail`
- Upload > 10 MB: → 413 com `ProblemDetail`
- Anexo opcional: `curl http://localhost:8080/api/sessions/1/attachments` → 200

### Critérios de aceite
- [ ] Upload de .txt e .pdf funcional com sessionId obrigatório
- [ ] Erros retornam `ProblemDetail` com `status`, `title`, `detail`
- [ ] Controller converte `MultipartFile` antes de chamar service
- [ ] Nenhuma regra de negócio no controller

### Definition of Done
- Commit com `AttachmentController`

---

## Fase 10: Integração Final e Validação

### Objetivo
Validar o sistema completo e ajustar detalhes de integração.

### Dependências
- Todas as fases anteriores

### Arquivos e pacotes envolvidos
- `src/main/resources/application.yml` (possíveis ajustes)
- Todos os pacotes

### Tarefas de implementação
1. Verificar se todos os endpoints funcionam conforme especificação
2. Validar ordenação de sessões (`updatedAt` desc) e mensagens (`timestamp` asc)
3. Confirmar que controllers não contêm regras de negócio
4. Confirmar que services não dependem de objetos HTTP
5. Validar que erros retornam `ProblemDetail` com campos corretos
6. Ajustar diretório de upload (ex: `./uploads/`) e criar `.gitkeep` se necessário
7. Configurar CORS no `application.yml` ou via `@Configuration` (permitir `http://localhost:5173` para Vite)

### Contratos da API afetados
- Todos

### Validações necessárias
- Todos os critérios de aceite da especificação

### Testes manuais recomendados
- Fluxo completo: criar sessão → enviar mensagens → ver histórico → fazer upload → verificar anexos
- Testar health check
- Testar erros (sessão inexistente, arquivo inválido, etc.)

### Critérios de aceite
- Todos os itens da seção "Critérios de aceite da primeira etapa" da especificação

### Definition of Done
- Aplicação funcional ponta a ponta
- Checklist final assinado

---

## Seção Final

### Dependências entre fases
```
Fase 1 (bootstrap)
  └── Fase 2 (entidades)
        ├── Fase 3 (DTOs + erro)
        │     └── ...
        └── Fase 4 (repositories)
              ├── Fase 5 (services: sessão/mensagem)
              │     ├── Fase 6 (chat service)
              │     │     └── Fase 7 (controllers: health/sessão/chat)
              │     └── ...
              └── Fase 8 (attachment service)
                    └── Fase 9 (attachment controller)
Fase 10 (integração final) — depende de todas
```

### Tarefas em paralelo
- **Fase 3 (DTOs + erro)** pode ser feita em paralelo com **Fase 4 (repositories)**
- **Fase 7 (controllers: health/sessão/chat)** usa apenas services das fases 5 e 6
- **Fase 9 (attachment controller)** depende apenas da fase 8

### Tarefas que dependem do outro repositório
- Nenhuma — back-end é independente, contratos da API já estão definidos e aprovados
- O front-end pode consumir os endpoints assim que a Fase 7 estiver completa

### Riscos de integração
- CORS não configurado pode bloquear requisições do Vite (porta 5173) — resolver na Fase 10
- Diferença de nomes de campos entre o `ProblemDetail` esperado pelo front-end e o retornado
- Timezone/Instant: garantir que o back-end serialize `Instant` corretamente no formato ISO 8601 UTC (configurar `spring.jackson.serialization.write-dates-as-timestamps=false` e `spring.jackson.time-zone=UTC`)

### Checklist final da primeira etapa
- [ ] `GET /api/health` → 200
- [ ] `POST /api/sessions` → 201
- [ ] `GET /api/sessions` → 200 (ordenado por `updatedAt` desc)
- [ ] `GET /api/sessions/{id}` → 200 ou 404
- [ ] `POST /api/chat` → 201 com `userMessage` e `assistantMessage`
- [ ] `POST /api/chat` com sessionId inválido → 404
- [ ] `GET /api/sessions/{id}/messages` → 200 (ordenado por `timestamp` asc) ou 404
- [ ] `POST /api/upload` (TXT/PDF válido, sessionId obrigatório) → 201
- [ ] `POST /api/upload` tipo inválido → 400 com `ProblemDetail`
- [ ] `POST /api/upload` > 10 MB → 413 com `ProblemDetail`
- [ ] Controllers sem regras de negócio
- [ ] Services sem dependências HTTP
- [ ] DTOs imutáveis
- [ ] Erros usam `ProblemDetail` com `status`, `title`, `detail`

### Sugestão de branches por funcionalidade
| Branch | Conteúdo |
|--------|----------|
| `main` | versão estável |
| `feature/bootstrap` | Fase 1 |
| `feature/entities` | Fase 2 |
| `feature/dtos-error-handling` | Fase 3 |
| `feature/repositories` | Fase 4 |
| `feature/session-message-services` | Fase 5 |
| `feature/chat-service` | Fase 6 |
| `feature/controllers` | Fase 7 |
| `feature/attachment-service` | Fase 8 |
| `feature/upload-controller` | Fase 9 |
| `feature/integration` | Fase 10 |
