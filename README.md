# MindJourney IA — Back-end

API back-end do MindJourney IA, um diário inteligente que permite registrar conversas em sessões, enviar arquivos e consultar o histórico com respostas contextuais baseadas no conteúdo dos documentos enviados.

O projeto implementa um pipeline RAG (Retrieval-Augmented Generation): arquivos enviados são processados (parsing, chunking, embeddings vetoriais), indexados no PostgreSQL com pgvector e consultados semanticamente durante o chat para fundamentar as respostas do assistente.

## Funcionalidades

- Criação, listagem e consulta de sessões de diário
- Chat com respostas do assistente (mock ou geradas por IA via Ollama)
- Upload de arquivos `.txt` e `.pdf` com ingestão automática
- Pipeline RAG: parsing → chunking → embeddings → indexação vetorial
- Geração contextual de respostas usando `llama3.2:3b`
- Busca semântica com similaridade por cosseno (pgvector)
- Retorno de fontes consultadas na resposta do chat
- Notificação opcional via webhook n8n ao final da indexação
- Perfil H2 para desenvolvimento sem dependências externas
- Perfil PostgreSQL para pipeline RAG completo

## Tecnologias

| Categoria | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.4.4, Spring Web, Spring Data JPA, Bean Validation |
| Build | Maven + Maven Wrapper |
| Banco (dev) | H2 em arquivo |
| Banco (RAG) | PostgreSQL 17 + pgvector |
| Migrações | Flyway |
| Vetores | Hibernate Vector 6.6.11.Final (`hibernate-vector`) |
| PDF | Apache PDFBox 3.0.4 |
| Embeddings | Ollama + `embeddinggemma:300m` (768 dimensões) |
| Geração | Ollama + `llama3.2:3b` |
| Workflow | n8n (opcional) |
| Testes | JUnit 5, Testcontainers (PostgreSQL) |
| Parsing de datas | `java.time.Instant` (UTC) |

## Pré-requisitos

- **Git**
- **Java 17** (ou superior)
- **Docker Desktop** (para PostgreSQL com pgvector e para Testcontainers)
- **Ollama** (para embeddings e geração contextual)
- **PowerShell** (apenas para executar o script de validação E2E)

O Maven não precisa ser instalado globalmente — o projeto possui Maven Wrapper (`mvnw` / `mvnw.cmd`).

```bash
java -version
git --version
docker --version
ollama --version
```

## Clonar o repositório

```bash
git clone https://github.com/laislaferrari/ai-back-end-g2.git
cd ai-back-end-g2
```

## Perfis de execução

O Spring Boot utiliza perfis para ativar diferentes configurações e implementações:

### `default` (H2)

Ativado quando nenhum perfil é especificado.

- Banco H2 em arquivo
- Respostas do assistente geradas pelo `MockAiResponseGenerator` (baseadas em palavras-chave)
- Sem embeddings, sem pgvector, sem RAG
- Ideal para desenvolvimento rápido sem dependências externas

### `postgres`

Ativado com `--spring-boot.run.profiles=postgres` ou variável `SPRING_PROFILES_ACTIVE=postgres`.

- PostgreSQL + pgvector
- Flyway executa as migrações automaticamente
- Embeddings gerados pelo `OllamaEmbeddingService` (`embeddinggemma:300m`)
- Respostas geradas pelo `OllamaAiResponseGenerator` (`llama3.2:3b`)
- Pipeline RAG completo com ingestão e retrieval
- Notificação opcional ao n8n após indexação
- Necessário ter Docker, Ollama e PostgreSQL rodando

### `test`

Ativado automaticamente durante a execução dos testes.

- `MemoryEmbeddingService`: embeddings determinísticos em memória (sem Ollama)
- `MockAiResponseGenerator`: respostas mock (sem Ollama)
- Testcontainers provê PostgreSQL + pgvector para os testes de integração

## Executar com H2 (perfil default — sem Docker)

### Windows PowerShell

```powershell
.\mvnw.cmd spring-boot:run
```

### macOS / Linux / Git Bash

```bash
./mvnw spring-boot:run
```

A API será iniciada em `http://localhost:8080`.

O console do H2 fica disponível em `http://localhost:8080/h2-console`.

```
JDBC URL: jdbc:h2:file:./data/mindjournal
User Name: sa
Password: (vazio)
```

A pasta `data/` contém os arquivos do banco H2 e **não deve ser versionada**.

## Executar com PostgreSQL e pipeline RAG

### 1. Criar o arquivo `.env`

#### Windows PowerShell

```powershell
copy .env.example .env
```

#### macOS / Linux / Git Bash

```bash
cp .env.example .env
```

Edite o arquivo `.env` com os valores desejados. Todas as variáveis disponíveis:

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `POSTGRES_DB` | sim | `mindjournal` | Nome do banco no PostgreSQL |
| `DB_URL` | sim | `jdbc:postgresql://localhost:5433/mindjournal` | URL JDBC (porta 5433 mapeada no Docker) |
| `DB_USERNAME` | sim | `mindjournal` | Usuário do banco |
| `DB_PASSWORD` | sim | `mindjournal` | Senha do banco (alterar em produção) |
| `SPRING_PROFILES_ACTIVE` | sim | `postgres` | Ativa o perfil PostgreSQL |
| `OLLAMA_URL` | não | `http://localhost:11434/api/embed` | URL do endpoint de embeddings do Ollama |
| `OLLAMA_MODEL` | não | `embeddinggemma:300m` | Modelo de embedding |
| `OLLAMA_CHAT_URL` | não | `http://localhost:11434/api/chat` | URL do endpoint de chat do Ollama |
| `OLLAMA_CHAT_MODEL` | não | `llama3.2:3b` | Modelo de geração de texto |
| `OLLAMA_CHAT_TEMPERATURE` | não | `0.2` | Temperatura do modelo (0.0 a 2.0) |
| `RAG_TOP_K` | não | `3` | Número de fragmentos retornados na busca |
| `RAG_MIN_SIMILARITY` | não | `0.70` | Similaridade mínima (0.0 a 1.0) |
| `N8N_DOCUMENT_INDEXED_URL` | não | vazio | URL do webhook n8n (deixar vazio se não usar n8n) |

O arquivo `.env` **não deve ser versionado**.

### 2. Instalar e configurar os modelos do Ollama

Instale o Ollama conforme seu sistema operacional:

- **macOS:** `brew install ollama`
- **Linux:** `curl -fsSL https://ollama.com/install.sh | sh`
- **Windows:** Baixe o instalador em [ollama.com](https://ollama.com)

Inicie o servidor Ollama:

```bash
ollama serve
```

Em outro terminal, baixe os modelos necessários:

```bash
ollama pull embeddinggemma:300m
ollama pull llama3.2:3b
```

Verifique se os modelos estão disponíveis:

```bash
ollama list
```

### 3. Iniciar o PostgreSQL com Docker Compose

```bash
docker compose up -d
```

Isso inicia um container PostgreSQL 17 com a extensão pgvector. A porta `5433` do host é mapeada para a porta `5432` do container.

Verifique se o banco está pronto:

```bash
docker compose ps
```

### 4. Carregar as variáveis do `.env`

O Spring Boot não lê o arquivo `.env` automaticamente. Carregue as variáveis no terminal antes de executar a aplicação.

#### Windows PowerShell

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#][^=]*)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable(
            $matches[1].Trim(),
            $matches[2].Trim(),
            'Process'
        )
    }
}
```

#### macOS / Linux / Git Bash

```bash
set -a
source .env
set +a
```

### 5. Executar o backend com PostgreSQL

#### Windows PowerShell

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

#### macOS / Linux / Git Bash

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

As migrações do Flyway (V1, V2, V3) serão executadas automaticamente ao iniciar.

### 6. Testar o endpoint health

```bash
curl http://localhost:8080/api/health
```

Resposta esperada:

```json
{"status":"UP","timestamp":"2026-06-30T00:00:00Z"}
```

## Executar com n8n (opcional)

O repositório contém um workflow n8n em `n8n/workflows/document-indexed-notification.json` que recebe a notificação de indexação e consulta o status do documento.

1. Inicie o n8n (Docker ou local)
2. Importe o arquivo `n8n/workflows/document-indexed-notification.json`
3. Publique o workflow
4. Copie a URL do webhook gerada e defina em `N8N_DOCUMENT_INDEXED_URL` no `.env`

O webhook recebe um payload com `event`, `documentId`, `fileName`, `status`, `indexedChunks` e `timestamp`. O workflow consulta `GET /api/health` e `GET /api/documents/{documentId}/status` usando `http://host.docker.internal:8080/api` como URL base.

A notificação ao n8n é opcional. Se a URL não for configurada, a indexação continua normalmente e apenas um log informativo é gerado.

## Pipeline RAG (visão geral)

### 1. Upload e ingestão

```
POST /api/upload (multipart: file + sessionId)
  → Attachment salva o arquivo em disco
  → Cria Document (status=RECEIVED)
  → DocumentIngestionService.ingest()
    → status = PROCESSING
    → leitura do arquivo do disco
    → SimpleDocumentParser.parse() — extrai texto (TXT ou PDF)
    → FixedSizeTextChunker.chunk() — fragmenta com overlap
    → OllamaEmbeddingService.generateEmbedding() — gera vetor 768d
    → IngestionTransactionService.replaceChunksAndMarkIndexed()
    → status = INDEXED
    → N8nWebhookDocumentIndexingNotifier (opcional)
```

### 2. Chat com retrieval

```
POST /api/chat (JSON: { sessionId, content })
  → salva mensagem do usuário
  → RagService.retrieveContext()
    → gera embedding da pergunta
    → busca similaridade no pgvector (cosine_distance)
    → retorna fragmentos com score >= minSimilarity
  → AiResponseGenerator.generateResponse(contexto + pergunta)
    → Ollama ou Mock conforme o perfil
  → salva resposta do assistente
  → retorna ChatResponse com userMessage, assistantMessage e sources
```

## Endpoints

### Health

```
GET /api/health
```

Resposta (200):

```json
{
  "status": "UP",
  "timestamp": "2026-06-30T00:00:00Z"
}
```

### Sessões

```
POST /api/sessions       → 201 Created
GET  /api/sessions       → 200 OK
GET  /api/sessions/{id}  → 200 OK
GET  /api/sessions/{id}/messages  → 200 OK
```

**Criar sessão:**

```json
// Request POST /api/sessions
{ "title": "Meu diário de hoje" }

// Response 201
{
  "id": 1,
  "title": "Meu diário de hoje",
  "createdAt": "2026-06-30T10:00:00Z",
  "updatedAt": "2026-06-30T10:00:00Z"
}
```

- `title`: obrigatório, máximo 150 caracteres
- `id`: gerado automaticamente
- Datas em UTC (Instant)

**Listar sessões:** retorna array ordenado por `updatedAt` decrescente.

**Buscar sessão por ID:** retorna `SessionResponse` ou `404`.

**Mensagens da sessão:** retorna array de `MessageResponse` ordenado por `timestamp` crescente ou `404` se a sessão não existir.

```json
// Response GET /api/sessions/1/messages
[
  {
    "id": 1,
    "content": "Estou me sentindo bem hoje",
    "role": "USER",
    "timestamp": "2026-06-30T10:00:00Z"
  },
  {
    "id": 2,
    "content": "Que notícia excelente!",
    "role": "ASSISTANT",
    "timestamp": "2026-06-30T10:00:01Z"
  }
]
```

### Chat

```
POST /api/chat  → 201 Created
```

```json
// Request
{ "sessionId": 1, "content": "Qual é a cor secreta?" }

// Response 201
{
  "userMessage": {
    "id": 3,
    "content": "Qual é a cor secreta?",
    "role": "USER",
    "timestamp": "2026-06-30T10:05:00Z"
  },
  "assistantMessage": {
    "id": 4,
    "content": "A cor secreta mencionada nos seus documentos é violeta.",
    "role": "ASSISTANT",
    "timestamp": "2026-06-30T10:05:03Z"
  },
  "sources": [
    {
      "documentId": 1,
      "fileName": "teste.txt",
      "chunkId": 3,
      "content": "A cor secreta utilizada no teste de recuperação contextual é violeta.",
      "similarityScore": 0.89
    }
  ]
}
```

- `sessionId`: obrigatório
- `content`: obrigatório, não pode ser vazio
- `sources`: sempre presente (`[]` quando não houver documentos indexados ou similaridade insuficiente)

**SourceDTO:**

| Campo | Tipo | Descrição |
|---|---|---|
| `documentId` | Long | ID do documento indexado |
| `fileName` | String | Nome original do arquivo |
| `chunkId` | Long | ID do fragmento (chunk) |
| `content` | String | Conteúdo do fragmento |
| `similarityScore` | double | Score de similaridade (0.0 a 1.0) |

### Upload

```
POST /api/upload  → 201 Created
```

Requisição `multipart/form-data` com os campos:

| Campo | Tipo | Obrigatório |
|---|---|---|
| `file` | arquivo | sim (.txt ou .pdf, máx. 10 MB) |
| `sessionId` | Long | sim |

Response (201):

```json
{
  "id": 1,
  "sessionId": 1,
  "filename": "diario.txt",
  "type": "TXT",
  "size": 1024,
  "uploadDate": "2026-06-30T10:00:00Z",
  "documentId": 1
}
```

- `documentId`: ID do `Document` criado — use para consultar o status da indexação
- Formatos aceitos: `.txt` (text/plain) e `.pdf` (application/pdf)
- Limite: **10 MB** por arquivo

### Status do documento

```
GET /api/documents/{id}/status  → 200 OK
```

```json
{
  "documentId": 1,
  "fileName": "diario.txt",
  "status": "INDEXED",
  "updatedAt": "2026-06-30T10:05:00Z"
}
```

Status possíveis: `RECEIVED` → `PROCESSING` → `INDEXED` | `FAILED`.

Retorna `404` se o documento não existir.

## Ordem de inicialização

Para usar o pipeline RAG completo, siga a ordem abaixo:

1. **PostgreSQL/pgvector:** `docker compose up -d`
2. **Ollama:** `ollama serve` (modelos já baixados com `ollama pull`)
3. **n8n** (opcional): inicie e publique o workflow
4. **Backend:** execute com o perfil `postgres`

## Estrutura do projeto

```
src/main/java/com/mindjournal/
├── config/
│   ├── CorsConfig.java
│   ├── RagPersistenceConfig.java
│   ├── RagIngestionConfig.java
│   ├── RagIngestionProperties.java
│   ├── RagRetrievalProperties.java
│   └── OllamaGenerationProperties.java
├── controller/
│   ├── HealthController.java
│   ├── SessionController.java
│   ├── ChatController.java
│   ├── AttachmentController.java
│   └── DocumentController.java
├── dto/
│   ├── HealthDTO.java
│   ├── SessionResponse.java
│   ├── CreateSessionRequest.java
│   ├── MessageResponse.java
│   ├── ChatRequest.java
│   ├── ChatResponse.java
│   ├── SourceDTO.java
│   ├── AttachmentDTO.java
│   ├── AttachmentInput.java
│   └── DocumentStatusResponse.java
├── entity/
│   ├── Session.java
│   ├── Message.java
│   ├── MessageRole.java
│   ├── Attachment.java
│   ├── AttachmentType.java
│   ├── Document.java
│   └── DocumentStatus.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── SessionNotFoundException.java
│   ├── DocumentNotFoundException.java
│   ├── InvalidFileException.java
│   ├── EmptyDocumentException.java
│   ├── ParsingException.java
│   ├── EmbeddingException.java
│   └── GenerationException.java
├── repository/
│   ├── SessionRepository.java
│   ├── MessageRepository.java
│   ├── AttachmentRepository.java
│   └── DocumentRepository.java
├── service/
│   ├── SessionService.java
│   ├── MessageService.java
│   ├── ChatService.java
│   ├── AiResponseGenerator.java
│   ├── MockAiResponseGenerator.java
│   ├── OllamaAiResponseGenerator.java
│   ├── AttachmentService.java
│   ├── DocumentStatusService.java
│   ├── chunking/
│   │   ├── TextChunker.java
│   │   └── FixedSizeTextChunker.java
│   ├── embedding/
│   │   ├── EmbeddingService.java
│   │   ├── MemoryEmbeddingService.java
│   │   └── OllamaEmbeddingService.java
│   ├── parsing/
│   │   ├── DocumentParser.java
│   │   └── SimpleDocumentParser.java
│   └── rag/
│       ├── RagService.java
│       ├── RagContext.java
│       ├── DocumentIngestionService.java
│       ├── IngestionTransactionService.java
│       ├── IngestionSource.java
│       ├── PreparedChunk.java
│       ├── DocumentIndexingNotifier.java
│       └── N8nWebhookDocumentIndexingNotifier.java
└── MindJournalApplication.java

src/main/java/vector/rag/
├── entity/DocumentChunk.java
└── repository/
    ├── DocumentChunkRepository.java
    └── RelevantChunkProjection.java

src/main/resources/
├── application.yml
├── application-postgres.yml
└── db/migration/
    ├── V1__enable_pgvector.sql
    ├── V2__create_part1_tables.sql
    └── V3__create_rag_tables.sql

docs/
├── specs/
├── plans/
├── prompts/
└── parte-2/

n8n/workflows/
└── document-indexed-notification.json

scripts/
└── validate-rag-e2e.ps1
```

## Testes

### Executar todos os testes

#### Windows PowerShell

```powershell
.\mvnw.cmd clean test
```

#### macOS / Linux / Git Bash

```bash
./mvnw clean test
```

Os testes de integração que utilizam pgvector e Hibernate Vector dependem do **Testcontainers**, que por sua vez exige o **Docker** rodando. O Testcontainers gerencia automaticamente um container PostgreSQL + pgvector para os testes.

### Script de validação E2E

O repositório contém um script PowerShell que valida o pipeline RAG completo:

```powershell
.\scripts\validate-rag-e2e.ps1
```

O script executa:

1. `GET /api/health` — verifica se a API está ativa
2. `POST /api/sessions` — cria uma sessão de teste
3. Cria um arquivo TXT local com conteúdo específico
4. `POST /api/upload` — envia o arquivo para a sessão
5. `GET /api/documents/{id}/status` — faz polling até o status ser `INDEXED` (máx. 60s)
6. `POST /api/chat` — pergunta sobre a cor secreta contida no documento
7. Valida que a resposta contém a palavra "violeta" e que o campo `sources` está preenchido

Resultado esperado:

```
##############################
#        TESTE APROVADO       #
##############################
```

Pré-requisitos para o script E2E:

- Backend rodando com perfil `postgres`
- PostgreSQL acessível (Docker Compose)
- Ollama rodando com os modelos baixados
- PowerShell 5.1 ou superior

Para usar uma URL base diferente:

```powershell
.\scripts\validate-rag-e2e.ps1 -BaseUrl "http://localhost:8080"
```

## Troubleshooting

### Profile postgres não ativo
A aplicação inicia mas o chat retorna respostas mock e o pipeline RAG não executa. Verifique se a variável `SPRING_PROFILES_ACTIVE=postgres` foi carregada antes da execução. Use `curl http://localhost:8080/api/health` e confira se os serviços RAG estão disponíveis nos logs.

### PostgreSQL indisponível
Erro de conexão ao iniciar com profile `postgres`. Execute `docker compose ps` para verificar se o container está rodando. Verifique se a porta `5433` não está ocupada. Se necessário, recrie o volume com `docker compose down -v && docker compose up -d`.

### Senha diferente do volume existente
Se o banco foi criado com uma senha e o `.env` foi alterado depois, o PostgreSQL ignora a nova senha. Remova o volume: `docker compose down -v && docker compose up -d`.

### Ollama indisponível
Erro "Connection refused" ao fazer upload de arquivo ou enviar mensagem. Confirme que `ollama serve` está rodando. Teste com `curl http://localhost:11434/api/embed`.

### Modelo ausente
Erro "model not found" do Ollama. Execute `ollama pull embeddinggemma:300m` e `ollama pull llama3.2:3b`.

### Embedding com dimensão diferente de 768
Erro na ingestão com mensagem de dimensão inválida. Verifique se o modelo configurado em `OLLAMA_MODEL` gera vetores de 768 dimensões. O `embeddinggemma:300m` é o modelo validado.

### Documento não chega a INDEXED
O documento permanece em `RECEIVED` ou `PROCESSING`. Verifique os logs do backend para identificar a etapa que falhou (leitura, parsing, chunking, embedding, persistência). Confirme que o Ollama está acessível.

### Sources vazias na resposta do chat
O campo `sources` retorna `[]`. Verifique se existem documentos com status `INDEXED` na sessão. Reduza `RAG_MIN_SIMILARITY` ou aumente `RAG_TOP_K` no `.env` se necessário.

### Webhook do n8n não configurado
A indexação funciona normalmente mesmo sem n8n. O log exibirá "URL do webhook n8n não configurada — notificação ignorada". Para ativar, defina `N8N_DOCUMENT_INDEXED_URL` no `.env`.

### Porta 8080 ocupada
Altere a porta no `application.yml` ou encerre o processo que está usando a porta 8080.

## Parar os containers

```bash
docker compose down
```

Para parar e remover também os dados do banco:

```bash
docker compose down -v
```
