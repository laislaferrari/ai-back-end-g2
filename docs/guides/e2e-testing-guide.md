# Teste E2E do Pipeline RAG

## Objetivo

Validar o pipeline RAG completo do MindJourney IA, desde o health check até a resposta contextual do assistente, incluindo a criação de sessão, upload de arquivo, indexação e retrieval semântico.

## Pré-requisitos

Antes de executar o teste, verifique:

- Backend rodando com o perfil `postgres`
- PostgreSQL acessível (Docker Compose)
- Ollama rodando com os modelos `embeddinggemma:300m` e `llama3.2:3b` baixados
- PowerShell 5.1 ou superior (Windows) ou PowerShell 7 (macOS, Linux)

O n8n é opcional e não é necessário para o teste E2E.

## Ordem de inicialização

Os serviços devem ser iniciados na seguinte ordem:

1. **PostgreSQL/pgvector:** `docker compose up -d`
2. **Ollama:** inicie o Ollama e baixe os modelos (`ollama pull embeddinggemma:300m` e `ollama pull llama3.2:3b`)
3. **n8n** (opcional): inicie e publique o workflow de notificação
4. **Backend:** execute com o perfil `postgres`

## Executar o teste

### Windows PowerShell

```powershell
.\scripts\validate-rag-e2e.ps1
```

### macOS / Linux / Git Bash (PowerShell 7)

O script é escrito em PowerShell. No macOS e Linux, utilize o PowerShell 7 (`pwsh`):

```bash
pwsh scripts/validate-rag-e2e.ps1
```

## Parâmetros

O script aceita um único parâmetro opcional:

| Parâmetro | Tipo | Padrão | Descrição |
|---|---|---|---|
| `-BaseUrl` | string | `http://localhost:8080` | URL base do backend (sem `/api`) |

Exemplo com URL personalizada:

```powershell
.\scripts\validate-rag-e2e.ps1 -BaseUrl "http://localhost:8080"
```

O script adiciona `/api` automaticamente à URL fornecida.

## Fluxo validado

O script executa 10 passos sequenciais:

### Passo 1 — Health check

Requisição `GET /api/health`. O script verifica se o campo `status` retorna `"UP"`. Se falhar, o teste é interrompido com exit code 1.

### Passo 2 — Criar sessão

Requisição `POST /api/sessions` com o título `"Teste Final RAG Violeta"`. O ID da sessão retornado é armazenado para os passos seguintes.

### Passo 3 — Criar arquivo TXT de teste

Um arquivo temporário UTF-8 sem BOM é criado com o conteúdo:

```
Teste de recuperacao contextual do MindJournal.

O projeto MindJourney foi desenvolvido pelo Grupo 2.
A cor secreta utilizada no teste de recuperacao contextual e violeta.
Esta informacao deve ser recuperada pelo pipeline RAG a partir deste documento.
```

### Passo 4 — Upload do arquivo

Requisição `POST /api/upload` (multipart/form-data) enviando o arquivo criado e o `sessionId`. O `documentId` retornado na resposta é armazenado para o polling.

### Passo 5 — Polling do status

Requisições `GET /api/documents/{documentId}/status` a cada 2 segundos, até o status ser `INDEXED` ou até 30 tentativas (60 segundos).

- Se `INDEXED`: o teste prossegue.
- Se `FAILED`: o teste é interrompido com exit code 1.
- Se o tempo esgotar sem `INDEXED`: o teste é interrompido com exit code 1.

### Passo 6 — Enviar mensagem para o chat

Requisição `POST /api/chat` com a pergunta:

```
Qual e a cor secreta utilizada no teste de recuperacao contextual?
```

### Passo 7 — Diagnóstico da resposta

O script exibe o conteúdo da resposta do assistente e a lista de `sources` retornadas, incluindo `documentId`, `fileName`, `chunkId`, `content` e `similarityScore` de cada fonte.

### Passo 8 — Validação

Duas verificações são realizadas:

1. A resposta do assistente contém a palavra "violeta" (case insensitive).
2. O array `sources` não está vazio (ao menos uma fonte foi retornada).

Se alguma das duas falhar, o teste prossegue para o veredito final com a falha registrada.

### Passo 9 — Resumo final

O script exibe um resumo com `Session ID`, `Document ID`, status final, conteúdo da resposta do assistente e detalhes da primeira fonte (filename, chunkId, content, similarityScore).

### Passo 10 — Veredito final

Com base nas validações do passo 8, o script exibe o resultado.

## Interpretar o resultado

### TESTE APROVADO

```
  ##############################
  #        TESTE APROVADO       #
  ##############################
```

Exit code: `0`

Significa que a resposta do assistente contém a palavra "violeta" e o pipeline retornou ao menos uma fonte (source) com conteúdo relevante. O RAG está funcionando corretamente.

### TESTE REPROVADO

```
  ##############################
  #       TESTE REPROVADO       #
  ##############################
```

Exit code: `1`

Ocorre quando:

- A resposta do assistente não contém a palavra "violeta" (campo `assistantMessage.content`).
- O array `sources` está vazio (nenhum fragmento foi retornado pela busca semântica).

As falhas específicas são exibidas antes do veredito:

```
[FALHA] Resposta do assistente nao contem a palavra 'violeta'
[FALHA] Resposta nao possui sources
```

### Falhas anteriores ao veredito

O script também pode ser interrompido antes do passo 8 com exit code 1 se:

- Health check falhar (passo 1)
- Criação da sessão falhar (passo 2)
- Criação do arquivo falhar (passo 3)
- Upload falhar (passo 4)
- Status do documento for `FAILED` ou timeout no polling (passo 5)
- Envio da mensagem falhar (passo 6)

## Identificar IDs, similaridade e fonte no resultado

O passo 7 exibe o diagnóstico completo das fontes:

```
--- SOURCES (1) ---
  DocumentId:       1
  FileName:         teste.txt
  ChunkId:          3
  Content:          A cor secreta utilizada no teste de recuperacao contextual e violeta.
  SimilarityScore:  0.89
  ---
```

- **DocumentId**: ID do documento indexado no backend.
- **FileName**: nome original do arquivo enviado.
- **ChunkId**: ID do fragmento (chunk) que foi retornado pela busca.
- **Content**: conteúdo textual do fragmento que correspondeu à pergunta.
- **SimilarityScore**: score de similaridade entre o embedding da pergunta e o embedding do fragmento (0.0 a 1.0). Quanto mais próximo de 1.0, maior a similaridade semântica.

## Problemas comuns

### Profile postgres não ativo

A aplicação inicia com o perfil H2 (default) e o pipeline RAG não executa. O chat retorna respostas mock e o status do documento nunca chega a `INDEXED`.

Verifique se `SPRING_PROFILES_ACTIVE=postgres` foi carregado no terminal antes de iniciar o backend.

### PostgreSQL indisponível

Erro de conexão ao iniciar o backend ou timeout no health check.

Execute `docker compose ps` para verificar se o container PostgreSQL está rodando na porta 5433.

### Ollama não está rodando

O polling do documento fica preso em `RECEIVED` ou `PROCESSING` e nunca atinge `INDEXED`, ou o health check do Ollama falha com `Connection refused`.

Consulte o guia `docs/guides/ollama-setup.md` para verificar e iniciar o Ollama.

### Upload falha (formato ou tamanho)

O script envia um arquivo `.txt`. Se o upload falhar, verifique:

- O limite de 10 MB não foi excedido.
- O backend está com o perfil `postgres` ativo.
- O `sessionId` utilizado existe no banco.

### Polling atinge timeout sem INDEXED

O documento permanece em `RECEIVED` ou `PROCESSING` por mais de 60 segundos.

Causas possíveis:

- Ollama não está acessível (embedding não é gerado).
- Erro no parsing ou chunking (verifique os logs do backend).
- Dimensão do embedding incompatível (veja `docs/guides/ollama-setup.md`).

### Resposta não contém "violeta"

A resposta do assistente foi gerada mas não inclui a palavra esperada.

Causas possíveis:

- O documento não foi indexado corretamente (verifique o status no passo 5).
- A busca semântica não retornou o fragmento relevante (similaridade abaixo do `RAG_MIN_SIMILARITY`).
- O modelo de geração (`llama3.2:3b`) não utilizou o contexto fornecido.

### Sources vazias

O array `sources` retornou vazio, mesmo com o documento indexado.

Causas possíveis:

- `RAG_MIN_SIMILARITY` muito alto — reduza para `0.50` no `.env` e reinicie o backend.
- `RAG_TOP_K` muito baixo — aumente para `5` no `.env` e reinicie o backend.
- O embedding da pergunta não encontrou similaridade com nenhum fragmento indexado.

## Encerrar os serviços após o teste

### Parar o backend

Pressione `Ctrl+C` no terminal onde o backend está rodando.

### Parar o PostgreSQL

```powershell
docker compose down
```

Para remover também os dados do banco:

```powershell
docker compose down -v
```

### Parar o Ollama

- **Windows:** Feche o aplicativo Ollama ou pressione `Ctrl+C` no terminal onde `ollama serve` está rodando.
- **macOS/Linux:** Pressione `Ctrl+C` no terminal onde `ollama serve` está rodando.

### Parar o n8n (se utilizado)

Pressione `Ctrl+C` no terminal onde o n8n está rodando ou execute `docker stop n8n`.
