# Roteiro Operacional — MindJourney IA

## Visão geral do ambiente

O sistema completo do MindJourney IA é composto por:

| Componente | Tecnologia | Porta |
|---|---|---|
| PostgreSQL + pgvector | Docker (pgvector/pgvector:pg17) | 5433 |
| Ollama | Serviço local | 11434 |
| n8n (opcional) | Docker ou local | 5678 |
| Backend | Spring Boot 3 + Maven Wrapper | 8080 |
| Frontend | React + Vite | 5173 |

## Terminais e serviços necessários

Mantenha um terminal separado para cada serviço em execução:

1. **Terminal 1** — Ollama (`ollama serve`)
2. **Terminal 2** — Docker Compose (PostgreSQL)
3. **Terminal 3** — Backend (Spring Boot)
4. **Terminal 4** — Frontend (React + Vite)
5. **Terminal 5** (opcional) — n8n

Os terminais devem permanecer abertos enquanto os serviços estiverem rodando.

## Verificação dos programas instalados

### Windows PowerShell

```powershell
java -version
git --version
docker --version
ollama --version
node --version
npm --version
```

### macOS / Linux / Git Bash

```bash
java -version
git --version
docker --version
ollama --version
node --version
npm --version
```

Confirme que todos os comandos retornam versões. O Maven não precisa estar instalado globalmente — o projeto utiliza Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Conferência das branches e git status

Antes de iniciar, confira se você está na branch correta e sem alterações locais não commitadas.

### Windows PowerShell

```powershell
git branch
git status
git pull
```

### macOS / Linux / Git Bash

```bash
git branch
git status
git pull
```

Cada funcionalidade deve estar em sua própria branch (ex.: `feature/health-endpoint`, `feature/chat-api`). Pull requests são feitos para a branch `main`.

## Configuração do Ollama

Baixe e instale o Ollama. Para instruções detalhadas, consulte [ollama-setup.md](ollama-setup.md).

Modelos necessários:

```bash
ollama pull embeddinggemma:300m
ollama pull llama3.2:3b
```

Verifique os modelos baixados:

```bash
ollama list
```

Inicie o servidor Ollama. Mantenha este terminal aberto.

## Configuração do .env do backend

Crie o arquivo `.env` na raiz do projeto (onde está o `docker-compose.yml`):

### Windows PowerShell

```powershell
copy .env.example .env
```

### macOS / Linux / Git Bash

```bash
cp .env.example .env
```

Edite o arquivo `.env` com as configurações desejadas. As variáveis disponíveis estão documentadas em [../../README.md](../../README.md). O arquivo `.env` não deve ser versionado.

## Inicialização do PostgreSQL/pgvector

### Windows PowerShell

```powershell
docker compose up -d
docker compose ps
```

### macOS / Linux / Git Bash

```bash
docker compose up -d
docker compose ps
```

O container `mindjournal-postgres` será iniciado na porta `5433`. Confirme que o status é `healthy`.

## Inicialização do n8n (opcional)

O n8n é opcional. Para instruções detalhadas, consulte [n8n-setup.md](n8n-setup.md).

### Windows PowerShell

```powershell
docker run -it --rm --name n8n -p 5678:5678 n8nio/n8n
```

### macOS / Linux / Git Bash

```bash
docker run -it --rm --name n8n -p 5678:5678 n8nio/n8n
```

Após iniciar, importe e publique o workflow em `n8n/workflows/document-indexed-notification.json` e configure `N8N_DOCUMENT_INDEXED_URL` no `.env`.

## Carregamento das variáveis de ambiente

O Spring Boot não lê o arquivo `.env` automaticamente. Carregue as variáveis no terminal antes de executar o backend.

### Windows PowerShell

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

### macOS / Linux / Git Bash

```bash
set -a
source .env
set +a
```

## Execução dos testes do backend

Execute os testes automatizados do backend. Os testes de integração utilizam Testcontainers e exigem Docker rodando.

### Windows PowerShell

```powershell
.\mvnw.cmd clean test
```

### macOS / Linux / Git Bash

```bash
./mvnw clean test
```

## Inicialização do backend com profile postgres

### Windows PowerShell

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

### macOS / Linux / Git Bash

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

As migrações do Flyway (V1, V2, V3) serão executadas automaticamente. A API será iniciada em `http://localhost:8080`.

## Teste do health

### Windows PowerShell

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method Get
```

### macOS / Linux / Git Bash

```bash
curl http://localhost:8080/api/health
```

Resposta esperada:

```json
{"status":"UP","timestamp":"2026-06-30T00:00:00Z"}
```

## Configuração do frontend

O frontend do MindJourney IA está em um repositório separado. Clone e acesse o repositório do frontend.

Em desenvolvimento, o frontend utiliza a rota relativa `/api` e o proxy do Vite encaminha as requisições para `http://localhost:8080`.

Crie um arquivo `.env` na raiz do frontend:

```env
VITE_API_URL=http://localhost:8080/api
```

## Instalação, build e inicialização do frontend

### Windows PowerShell

```powershell
npm install
npm run build
npm run dev
```

### macOS / Linux / Git Bash

```bash
npm install
npm run build
npm run dev
```

O frontend será iniciado em `http://localhost:5173`.

## Teste manual do fluxo

Com o backend e o frontend rodando, realize o fluxo manual:

### 1. Criar sessão

Através da interface do frontend (ou via API), crie uma nova sessão com um título como "Teste manual".

### 2. Enviar mensagem

Digite uma mensagem no chat da sessão. A resposta será gerada pelo modelo `llama3.2:3b` do Ollama.

### 3. Fazer upload de arquivo

Envie um arquivo `.txt` ou `.pdf` (máximo 10 MB) para a sessão.

### 4. Acompanhar indexação

O pipeline RAG processa o arquivo: parsing, chunking, embeddings (via `embeddinggemma:300m`). O status do documento evolui de `RECEIVED` → `PROCESSING` → `INDEXED`.

Consulte o status via API:

### Windows PowerShell

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/documents/1/status" -Method Get
```

### macOS / Linux / Git Bash

```bash
curl http://localhost:8080/api/documents/1/status
```

### 5. Perguntar sobre o conteúdo do arquivo

No chat da sessão, faça uma pergunta cuja resposta esteja no arquivo enviado. A resposta deve ser fundamentada pelo contexto recuperado (RAG).

### 6. Verificar fontes (sources)

A resposta do chat inclui o campo `sources` com os fragmentos utilizados, contendo `documentId`, `fileName`, `chunkId`, `content` e `similarityScore`.

### 7. n8n (se configurado)

Se o n8n foi configurado, a indexação do documento dispara uma notificação para o webhook. Acompanhe a execução no painel do n8n em `http://localhost:5678`.

## Teste E2E

O script `scripts/validate-rag-e2e.ps1` automatiza todo o fluxo de teste do pipeline RAG. Para instruções detalhadas, consulte [e2e-testing-guide.md](e2e-testing-guide.md).

Com o backend rodando com perfil `postgres`:

### Windows PowerShell

```powershell
.\scripts\validate-rag-e2e.ps1
```

### macOS / Linux / Git Bash (PowerShell 7)

```bash
pwsh scripts/validate-rag-e2e.ps1
```

Resultado esperado:

```
  ##############################
  #        TESTE APROVADO       #
  ##############################
```

## Verificação básica de acessibilidade por teclado

Na interface do frontend (`http://localhost:5173`), verifique:

1. Navegação entre campos com a tecla **Tab**.
2. Ativação de botões com **Enter** ou **Espaço**.
3. Leitura de tela com **NVDA** (Windows) ou **VoiceOver** (macOS), se disponível.

## Encerramento seguro dos serviços

A ordem de encerramento deve ser a inversa da inicialização:

### 1. Frontend

Pressione `Ctrl+C` no terminal onde o frontend está rodando.

### 2. Backend

Pressione `Ctrl+C` no terminal onde o backend está rodando.

### 3. n8n (se utilizado)

Pressione `Ctrl+C` no terminal onde o n8n está rodando ou:

```powershell
docker stop n8n
```

### 4. Ollama

- **Windows:** Feche o aplicativo Ollama ou pressione `Ctrl+C` no terminal onde `ollama serve` está rodando.
- **macOS/Linux:** Pressione `Ctrl+C` no terminal onde `ollama serve` está rodando.

Instruções detalhadas em [ollama-setup.md](ollama-setup.md).

### 5. PostgreSQL

```powershell
docker compose down
```

Para remover também os dados do banco:

```powershell
docker compose down -v
```

Instruções detalhadas em [../../README.md#parar-os-containers](../../README.md#parar-os-containers).
