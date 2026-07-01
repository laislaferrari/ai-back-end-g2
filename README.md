# MindJourney IA — Back-end

API do **MindJourney IA**, um diário inteligente que organiza conversas em sessões, recebe arquivos TXT e PDF e produz respostas contextuais com apoio de RAG (*Retrieval-Augmented Generation*).

Os documentos enviados passam por parsing, fragmentação, geração de embeddings e indexação vetorial no PostgreSQL com pgvector. Durante o chat, os trechos mais relevantes podem ser recuperados e enviados ao modelo de IA para fundamentar a resposta.

> Este repositório contém o **back-end**. A infraestrutura de apoio é iniciada com Docker Compose, enquanto o Spring Boot é executado pelo Maven Wrapper. O front-end fica em um repositório separado.

## Funcionalidades

- Criação, listagem e consulta de sessões
- Exclusão completa de sessões
- Remoção das mensagens, documentos, anexos e arquivos físicos vinculados à sessão excluída
- Atualização manual do título da sessão
- Geração automática de título a partir da primeira mensagem do usuário
- Fallback de título quando o Ollama não estiver disponível
- Chat com respostas mock ou geradas por IA
- Campo `sessionTitle` na resposta do chat
- Upload de arquivos `.txt` e `.pdf`
- Consulta do status de indexação dos documentos
- Pipeline RAG: parsing → chunking → embeddings → indexação vetorial → retrieval
- Busca semântica com similaridade por cosseno
- Retorno das fontes consultadas na resposta do chat
- Notificação opcional por webhook n8n
- Perfil H2 para desenvolvimento rápido
- Perfil PostgreSQL para o fluxo RAG completo

## Tecnologias

| Categoria | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.4.4 |
| API | Spring Web e Bean Validation |
| Persistência | Spring Data JPA |
| Build | Maven + Maven Wrapper |
| Banco local simples | H2 em arquivo |
| Banco RAG | PostgreSQL 17 + pgvector |
| Migrações | Flyway |
| Vetores | Hibernate Vector |
| Parsing de PDF | Apache PDFBox |
| Embeddings | Ollama + `embeddinggemma:300m` |
| Geração de texto | Ollama + `llama3.2:3b` |
| Automação | n8n, opcional |
| Testes | JUnit 5, Mockito e Testcontainers |

## Repositórios

- Back-end: `https://github.com/laislaferrari/ai-back-end-g2`
- Front-end: `https://github.com/kevinsgoncalves/ai-front-end-g2`

## Pré-requisitos

### Para o modo rápido com H2

- Git
- Java 17 ou superior

### Para o modo completo com RAG

- Git
- Java 17 ou superior
- Docker Desktop aberto e com o Docker Engine ativo
- Node.js 18 ou superior para executar o front-end

O Maven não precisa ser instalado globalmente. O projeto possui Maven Wrapper:

- Windows: `mvnw.cmd`
- macOS, Linux ou Git Bash: `mvnw`

Confira as instalações:

```powershell
java -version
git --version
docker --version
node --version
npm --version
```

## Visão rápida dos modos de execução

| Modo | Banco | IA | RAG | Docker |
|---|---|---|---|---|
| H2 | H2 local | Resposta mock | Não | Não |
| PostgreSQL | PostgreSQL + pgvector | Ollama | Sim | Sim |
| Testes | H2 + containers temporários | Serviços de teste | Testes de integração | Necessário para Testcontainers |

---

# Início rápido — projeto completo

Esta é a forma recomendada para executar **PostgreSQL, pgvector, Ollama, n8n, back-end e front-end**.

## 1. Clonar os dois repositórios

Em uma pasta de trabalho:

```powershell
git clone https://github.com/laislaferrari/ai-back-end-g2.git
git clone https://github.com/kevinsgoncalves/ai-front-end-g2.git
```

A estrutura ficará semelhante a:

```text
IA/
├── ai-back-end-g2/
└── ai-front-end-g2/
```

## 2. Abrir o Docker Desktop

Abra o Docker Desktop e aguarde até o Docker Engine ficar ativo.

Confira:

```powershell
docker info
```

Se o comando exibir as informações do servidor Docker, ele está pronto.

## 3. Criar o `.env` do back-end

Entre na pasta do back-end:

```powershell
cd "C:\caminho\para\ai-back-end-g2"
```

No Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

No macOS, Linux ou Git Bash:

```bash
cp .env.example .env
```

Conteúdo esperado:

```env
POSTGRES_DB=mindjournal
DB_URL=jdbc:postgresql://localhost:5433/mindjournal
DB_USERNAME=mindjournal
DB_PASSWORD=troque_esta_senha
SPRING_PROFILES_ACTIVE=postgres

OLLAMA_URL=http://localhost:11434/api/embed
OLLAMA_MODEL=embeddinggemma:300m
OLLAMA_CHAT_URL=http://localhost:11434/api/chat
OLLAMA_CHAT_MODEL=llama3.2:3b
OLLAMA_CHAT_TEMPERATURE=0.2

N8N_DOCUMENT_INDEXED_URL=

RAG_TOP_K=3
RAG_MIN_SIMILARITY=0.70
```

### Como funciona a senha do PostgreSQL

A senha é definida no campo:

```env
DB_PASSWORD=troque_esta_senha
```

Escolha uma senha e mantenha o mesmo valor nesse arquivo. Você **não precisa digitar essa senha no terminal** ao executar o Docker Compose ou o back-end.

O valor é usado automaticamente por dois componentes:

1. O container `mindjournal-pgvector` usa a senha para criar o usuário do PostgreSQL.
2. O Spring Boot usa a mesma senha para se conectar ao banco.

O arquivo `.env` é local e não deve ser enviado para o GitHub.

> Se o volume do PostgreSQL já tiver sido criado e a senha for alterada depois, a senha antiga continuará armazenada no volume. Veja a seção de troubleshooting antes de remover volumes.

## 4. Subir a infraestrutura com Docker Compose

Dentro da pasta do back-end:

```powershell
docker compose up -d
```

Esse comando inicia três serviços:

| Serviço | Container | Porta local | Função |
|---|---|---:|---|
| `ollama` | `mindjournal-ollama` | `11434` | Embeddings, títulos e respostas da IA |
| `pgvector` | `mindjournal-pgvector` | `5433` | PostgreSQL 17 com extensão vector |
| `n8n` | `mindjournal-n8n` | `5678` | Automação opcional por webhook |

Confira o estado:

```powershell
docker compose ps
```

Para acompanhar os logs:

```powershell
docker compose logs -f
```

Para acompanhar somente um serviço:

```powershell
docker compose logs -f pgvector
docker compose logs -f ollama
docker compose logs -f n8n
```

## 5. Baixar os modelos dentro do container Ollama

Na primeira execução, baixe os dois modelos:

```powershell
docker compose exec ollama ollama pull embeddinggemma:300m
docker compose exec ollama ollama pull llama3.2:3b
```

Confira:

```powershell
docker compose exec ollama ollama list
```

Os modelos ficam armazenados no volume `ollama_data`, portanto não precisam ser baixados novamente a cada inicialização.

> Quando o Ollama é executado pelo Docker Compose, não rode `ollama serve` no Windows ao mesmo tempo. Os dois tentariam usar a porta `11434`.

## 6. Carregar o `.env` no terminal do back-end

O Docker Compose lê o `.env` automaticamente, mas o processo Spring Boot executado fora do Docker precisa receber essas variáveis no terminal.

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

As variáveis são carregadas somente nesse terminal. Ao abrir outro PowerShell, execute o bloco novamente.

### macOS, Linux ou Git Bash

```bash
set -a
source .env
set +a
```

## 7. Executar o back-end

No mesmo terminal em que o `.env` foi carregado:

### Windows PowerShell

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

### macOS, Linux ou Git Bash

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

Aguarde uma mensagem semelhante a:

```text
Started MindJournalApplication
```

O back-end ficará disponível em:

```text
http://localhost:8080
```

Teste o health check no navegador:

```text
http://localhost:8080/api/health
```

Ou no PowerShell:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

## 8. Executar o front-end

Abra um **segundo terminal**:

```powershell
cd "C:\caminho\para\ai-front-end-g2"
```

Na primeira execução:

```powershell
Copy-Item .env.example .env.local
npm install
```

O arquivo `.env.local` deve conter:

```env
VITE_API_URL=http://localhost:8080/api
```

Inicie o Vite:

```powershell
npm run dev
```

A interface ficará disponível em:

```text
http://localhost:5173
```

Se a porta `5173` estiver ocupada, use o endereço exibido pelo Vite no terminal.

## Endereços locais

| Componente | Endereço |
|---|---|
| Front-end | `http://localhost:5173` |
| Back-end | `http://localhost:8080` |
| Health | `http://localhost:8080/api/health` |
| Ollama | `http://localhost:11434` |
| PostgreSQL | `localhost:5433` |
| n8n | `http://localhost:5678` |
| H2 Console, somente perfil default | `http://localhost:8080/h2-console` |

---

# Inicializações seguintes

Depois que o projeto já tiver sido configurado uma vez, o fluxo diário é menor.

## Terminal 1 — infraestrutura

```powershell
cd "C:\caminho\para\ai-back-end-g2"
docker compose up -d
docker compose ps
```

## Terminal 2 — back-end

```powershell
cd "C:\caminho\para\ai-back-end-g2"

Get-Content .env | ForEach-Object {
    if ($_ -match '^([^#][^=]*)=(.*)$') {
        [System.Environment]::SetEnvironmentVariable(
            $matches[1].Trim(),
            $matches[2].Trim(),
            'Process'
        )
    }
}

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

## Terminal 3 — front-end

```powershell
cd "C:\caminho\para\ai-front-end-g2"
npm run dev
```

Depois acesse:

```text
http://localhost:5173
```

---

# O que o `docker-compose.yml` configura

O Docker Compose foi configurado para centralizar a infraestrutura externa do projeto e evitar instalações manuais de PostgreSQL, pgvector e n8n.

## Serviço `ollama`

```yaml
ollama:
  image: ollama/ollama
  container_name: mindjournal-ollama
  ports:
    - "11434:11434"
  volumes:
    - ollama_data:/root/.ollama
  restart: unless-stopped
```

- Usa a imagem oficial do Ollama.
- Expõe a API na porta `11434`.
- Persiste os modelos no volume `ollama_data`.
- Reinicia automaticamente, exceto quando for parado manualmente.

## Serviço `pgvector`

```yaml
pgvector:
  image: pgvector/pgvector:pg17
  container_name: mindjournal-pgvector
  env_file:
    - .env
  environment:
    POSTGRES_DB: ${POSTGRES_DB}
    POSTGRES_USER: ${DB_USERNAME}
    POSTGRES_PASSWORD: ${DB_PASSWORD}
  ports:
    - "5433:5432"
  volumes:
    - pgvector_data:/var/lib/postgresql/data
    - ./scripts/init-pgvector.sql:/docker-entrypoint-initdb.d/init-pgvector.sql
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME} -d ${POSTGRES_DB}"]
    interval: 5s
    timeout: 5s
    retries: 10
  restart: unless-stopped
```

- Usa PostgreSQL 17 com pgvector.
- Lê banco, usuário e senha do `.env`.
- Mapeia a porta interna `5432` para a porta `5433` do computador, evitando conflito com outro PostgreSQL local.
- Persiste os dados no volume `pgvector_data`.
- Executa `scripts/init-pgvector.sql` na criação inicial do banco.
- O script garante a extensão:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

- Possui healthcheck com `pg_isready`.

## Serviço `n8n`

```yaml
n8n:
  image: n8nio/n8n
  container_name: mindjournal-n8n
  ports:
    - "5678:5678"
  environment:
    N8N_PORT: 5678
    N8N_HOST: localhost
    GENERIC_TIMEZONE: America/Sao_Paulo
    TZ: America/Sao_Paulo
  volumes:
    - n8n_data:/home/node/.n8n
  restart: unless-stopped
```

- Disponibiliza o n8n em `http://localhost:5678`.
- Usa o fuso horário `America/Sao_Paulo`.
- Persiste workflows e configurações no volume `n8n_data`.
- É opcional para o funcionamento principal do chat e do RAG.

## Volumes persistentes

```yaml
volumes:
  ollama_data:
  pgvector_data:
  n8n_data:
```

Os volumes preservam:

- modelos baixados pelo Ollama;
- banco e vetores do PostgreSQL;
- configurações e workflows do n8n.

## O que o Compose não executa

O arquivo atual não cria containers para:

- o back-end Spring Boot;
- o front-end React/Vite.

Por isso, após `docker compose up -d`, ainda é necessário executar:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

E, em outro terminal:

```powershell
npm run dev
```

## Subir apenas os serviços necessários

Sem n8n:

```powershell
docker compose up -d pgvector ollama
```

Somente PostgreSQL/pgvector:

```powershell
docker compose up -d pgvector
```

Para usar o Ollama instalado localmente, não suba o serviço `ollama` do Compose:

```powershell
docker compose up -d pgvector n8n
ollama serve
```

Escolha apenas uma origem para a porta `11434`: container ou instalação local.

---

# Modo rápido com H2 — sem Docker

Este modo é indicado para testar sessões, títulos, mensagens, upload e regras da API sem o pipeline RAG completo.

## Windows PowerShell

```powershell
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
.\mvnw.cmd spring-boot:run
```

## macOS, Linux ou Git Bash

```bash
unset SPRING_PROFILES_ACTIVE
./mvnw spring-boot:run
```

Características:

- banco H2 em arquivo;
- respostas do assistente geradas pelo mock;
- sem embeddings;
- sem pgvector;
- sem retrieval RAG;
- não exige senha;
- não exige Docker.

A API será iniciada em:

```text
http://localhost:8080
```

## H2 Console

```text
http://localhost:8080/h2-console
```

Preencha:

```text
JDBC URL: jdbc:h2:file:./data/mindjournal
User Name: sa
Password: deixe vazio
```

A pasta `data/` não deve ser versionada.

---

# Perfis Spring

## `default`

- H2 em arquivo
- `MockAiResponseGenerator`
- Sem RAG
- Flyway desativado

## `postgres`

- PostgreSQL + pgvector
- Flyway ativado
- Ollama para embeddings e geração
- Pipeline RAG completo
- n8n opcional

## `test`

- Ativado nos testes conforme a configuração da suíte
- Serviços determinísticos para testes unitários
- Testcontainers nos testes de integração com PostgreSQL/pgvector

---

# Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `POSTGRES_DB` | `mindjournal` | Nome do banco criado no container |
| `DB_URL` | `jdbc:postgresql://localhost:5433/mindjournal` | URL JDBC usada pelo back-end |
| `DB_USERNAME` | `mindjournal` | Usuário do PostgreSQL |
| `DB_PASSWORD` | `mindjournal` no fallback da aplicação | Senha compartilhada entre container e back-end |
| `SPRING_PROFILES_ACTIVE` | `postgres` no `.env.example` | Ativa o perfil PostgreSQL |
| `OLLAMA_URL` | `http://localhost:11434/api/embed` | Endpoint de embeddings |
| `OLLAMA_MODEL` | `embeddinggemma:300m` | Modelo de embeddings de 768 dimensões |
| `OLLAMA_CHAT_URL` | `http://localhost:11434/api/chat` | Endpoint de chat |
| `OLLAMA_CHAT_MODEL` | `llama3.2:3b` | Modelo de resposta e título |
| `OLLAMA_CHAT_TEMPERATURE` | `0.2` | Temperatura da geração |
| `RAG_TOP_K` | `3` | Máximo de fragmentos recuperados |
| `RAG_MIN_SIMILARITY` | `0.70` | Similaridade mínima |
| `N8N_DOCUMENT_INDEXED_URL` | vazio | Webhook opcional do n8n |

---

# Fluxo RAG

## Upload e indexação

```text
POST /api/upload
  → salva o arquivo em disco
  → cria Attachment
  → cria Document com status RECEIVED
  → altera para PROCESSING
  → extrai o texto de TXT ou PDF
  → divide o texto em chunks com overlap
  → gera embeddings no Ollama
  → grava os vetores no PostgreSQL/pgvector
  → altera o status para INDEXED
  → notifica o n8n, quando configurado
```

## Chat com retrieval

```text
POST /api/chat
  → valida a sessão
  → verifica se é a primeira mensagem do usuário
  → gera e salva o título automático quando necessário
  → salva a mensagem do usuário
  → gera o embedding da pergunta
  → busca chunks semanticamente semelhantes
  → envia pergunta e contexto para o modelo
  → salva a resposta do assistente
  → atualiza a sessão
  → retorna mensagens, fontes e sessionTitle
```

---

# Endpoints

## Health

```http
GET /api/health
```

Resposta `200 OK`:

```json
{
  "status": "UP",
  "timestamp": "2026-07-01T12:00:00Z"
}
```

## Sessões

| Método | Rota | Resultado |
|---|---|---|
| POST | `/api/sessions` | Cria uma sessão |
| GET | `/api/sessions` | Lista as sessões por atualização mais recente |
| GET | `/api/sessions/{id}` | Consulta uma sessão |
| GET | `/api/sessions/{id}/messages` | Lista as mensagens da sessão |
| PATCH | `/api/sessions/{id}/title` | Atualiza manualmente o título |
| DELETE | `/api/sessions/{id}` | Exclui a sessão e seus dados vinculados |

### Criar sessão

```http
POST /api/sessions
Content-Type: application/json
```

```json
{
  "title": "Nova sessão"
}
```

Resposta `201 Created`:

```json
{
  "id": 1,
  "title": "Nova sessão",
  "createdAt": "2026-07-01T12:00:00Z",
  "updatedAt": "2026-07-01T12:00:00Z"
}
```

### Atualizar título

```http
PATCH /api/sessions/1/title
Content-Type: application/json
```

```json
{
  "title": "Finalização do MindJourney"
}
```

Resposta `200 OK`:

```json
{
  "id": 1,
  "title": "Finalização do MindJourney",
  "createdAt": "2026-07-01T12:00:00Z",
  "updatedAt": "2026-07-01T12:10:00Z"
}
```

Regras:

- o título é obrigatório;
- espaços nas extremidades são removidos;
- o limite é de 150 caracteres;
- sessão inexistente retorna `404`;
- título vazio ou inválido retorna `400`.

### Excluir sessão

```http
DELETE /api/sessions/1
```

Resposta:

```text
204 No Content
```

A exclusão remove:

- mensagens da sessão;
- documentos relacionados;
- registros de anexos;
- arquivos físicos armazenados;
- a própria sessão.

Sessão inexistente retorna `404`.

## Chat

```http
POST /api/chat
Content-Type: application/json
```

```json
{
  "sessionId": 1,
  "content": "Hoje comecei a finalizar o projeto MindJourney"
}
```

Resposta `201 Created`:

```json
{
  "userMessage": {
    "id": 1,
    "content": "Hoje comecei a finalizar o projeto MindJourney",
    "role": "USER",
    "timestamp": "2026-07-01T12:15:00Z"
  },
  "assistantMessage": {
    "id": 2,
    "content": "Vamos organizar os próximos passos do projeto.",
    "role": "ASSISTANT",
    "timestamp": "2026-07-01T12:15:03Z"
  },
  "sources": [],
  "sessionTitle": "Finalização do projeto MindJourney"
}
```

### Título automático

Na primeira mensagem do usuário:

1. o serviço tenta gerar um título de 3 a 7 palavras pelo Ollama;
2. pontuação final e aspas são removidas;
3. o limite máximo é de 150 caracteres;
4. caso o Ollama falhe, as primeiras palavras da mensagem são usadas como fallback;
5. o chat continua mesmo se a geração do título falhar.

O campo `sessionTitle` é retornado no `ChatResponse` para permitir que o front-end atualize a barra lateral imediatamente.

## Upload

```http
POST /api/upload
Content-Type: multipart/form-data
```

Campos:

| Campo | Tipo | Obrigatório |
|---|---|---|
| `file` | TXT ou PDF, até 10 MB | Sim |
| `sessionId` | Long | Sim |

Resposta `201 Created`:

```json
{
  "id": 1,
  "sessionId": 1,
  "filename": "diario.txt",
  "type": "TXT",
  "size": 1024,
  "uploadDate": "2026-07-01T12:20:00Z",
  "documentId": 1
}
```

## Status do documento

```http
GET /api/documents/{id}/status
```

Resposta:

```json
{
  "documentId": 1,
  "fileName": "diario.txt",
  "status": "INDEXED",
  "updatedAt": "2026-07-01T12:21:00Z"
}
```

Status possíveis:

```text
RECEIVED → PROCESSING → INDEXED
                       ↘ FAILED
```

---

# n8n opcional

O Compose disponibiliza o n8n em:

```text
http://localhost:5678
```

O workflow do projeto está em:

```text
n8n/workflows/document-indexed-notification.json
```

Para utilizar:

1. abra o n8n;
2. importe o arquivo JSON;
3. publique o workflow;
4. copie a URL do webhook;
5. defina a URL em `N8N_DOCUMENT_INDEXED_URL` no `.env`;
6. recarregue as variáveis e reinicie o back-end.

Sem esse valor, a indexação continua normalmente.

---

# Testes

## Executar toda a suíte

Abra o Docker Desktop antes de executar a suíte completa, pois os testes de integração usam Testcontainers.

### Windows PowerShell

```powershell
.\mvnw.cmd clean test
```

### macOS, Linux ou Git Bash

```bash
./mvnw clean test
```

O Testcontainers cria bancos temporários automaticamente. Você **não precisa digitar senha de PostgreSQL** durante os testes.

## Build sem iniciar a aplicação

```powershell
.\mvnw.cmd clean package
```

## Validação E2E do RAG

Com Docker Compose, modelos do Ollama e back-end PostgreSQL em execução:

```powershell
.\scripts\validate-rag-e2e.ps1
```

O script valida health, criação de sessão, upload, indexação, chat contextual e fontes retornadas.

---

# Comandos do Docker Compose

## Iniciar

```powershell
docker compose up -d
```

## Ver estado

```powershell
docker compose ps
```

## Ver logs

```powershell
docker compose logs -f
```

## Reiniciar

```powershell
docker compose restart
```

## Parar sem apagar os dados

```powershell
docker compose down
```

## Parar e apagar todos os volumes

```powershell
docker compose down -v
```

> `docker compose down -v` apaga o banco PostgreSQL, os embeddings, os modelos baixados pelo Ollama e os dados do n8n. Use apenas quando realmente quiser reiniciar a infraestrutura do zero.

---

# Troubleshooting

## `docker compose up -d` informa que o `.env` não existe

Crie o arquivo:

```powershell
Copy-Item .env.example .env
```

## Docker não está disponível

Abra o Docker Desktop e teste:

```powershell
docker info
```

## Container PostgreSQL não fica saudável

```powershell
docker compose ps
docker compose logs pgvector
```

Confirme que:

- `.env` existe;
- `POSTGRES_DB`, `DB_USERNAME` e `DB_PASSWORD` estão preenchidos;
- a porta `5433` não está sendo usada por outro processo.

## Senha do PostgreSQL foi alterada depois da criação do volume

O PostgreSQL aplica a senha na criação inicial do volume. Alterar somente o `.env` não altera a senha armazenada.

Para recriar o banco do zero:

```powershell
docker compose down -v
docker compose up -d
```

Esse comando apaga os dados existentes.

## Back-end não conecta ao PostgreSQL

Confira se o `.env` foi carregado no **mesmo terminal** em que o Maven foi executado:

```powershell
$env:SPRING_PROFILES_ACTIVE
$env:DB_URL
$env:DB_USERNAME
```

Não exiba nem compartilhe o valor de `$env:DB_PASSWORD`.

## Aplicação inicia com H2 quando deveria usar PostgreSQL

Confira:

```powershell
$env:SPRING_PROFILES_ACTIVE
```

O resultado deve ser:

```text
postgres
```

Execute o back-end com:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

## Porta `5433` ocupada

Verifique:

```powershell
netstat -ano | findstr :5433
```

Encerre o processo conflitante ou altere, de forma coordenada, o mapeamento do Compose e a variável `DB_URL`.

## Porta `11434` ocupada

Isso normalmente acontece quando o Ollama local e o container Ollama estão ativos ao mesmo tempo.

Escolha um deles:

```powershell
docker compose stop ollama
ollama serve
```

Ou encerre o Ollama local e use:

```powershell
docker compose start ollama
```

## Modelo não encontrado

```powershell
docker compose exec ollama ollama pull embeddinggemma:300m
docker compose exec ollama ollama pull llama3.2:3b
```

## Documento permanece em `RECEIVED` ou `PROCESSING`

Veja os logs:

```powershell
docker compose logs ollama
```

E confira o terminal do back-end para identificar falha no parsing, embedding ou banco.

## Testcontainers não encontra o Docker

Erro comum:

```text
Could not find a valid Docker environment
```

Abra o Docker Desktop, aguarde o Engine ficar ativo e execute novamente:

```powershell
docker info
.\mvnw.cmd clean test
```

## Porta `8080` ocupada

```powershell
netstat -ano | findstr :8080
```

Encerre o processo anterior ou altere a porta da aplicação.

## Porta `5173` ocupada

O Vite escolherá outra porta e mostrará o endereço correto no terminal.

---

# Estrutura principal

```text
src/main/java/com/mindjournal/
├── config/
├── controller/
│   ├── HealthController.java
│   ├── SessionController.java
│   ├── ChatController.java
│   ├── AttachmentController.java
│   └── DocumentController.java
├── dto/
│   ├── CreateSessionRequest.java
│   ├── UpdateTitleRequest.java
│   ├── SessionResponse.java
│   ├── ChatRequest.java
│   ├── ChatResponse.java
│   ├── MessageResponse.java
│   ├── SourceDTO.java
│   ├── AttachmentDTO.java
│   └── DocumentStatusResponse.java
├── entity/
├── exception/
├── repository/
└── service/
    ├── SessionService.java
    ├── ChatService.java
    ├── TitleGeneratorService.java
    ├── AttachmentService.java
    ├── embedding/
    ├── parsing/
    ├── chunking/
    └── rag/

src/main/resources/
├── application.yml
├── application-postgres.yml
└── db/migration/

scripts/
├── init-pgvector.sql
└── validate-rag-e2e.ps1

n8n/workflows/
└── document-indexed-notification.json
```

---

# Boas práticas de segurança

- Não envie `.env` para o GitHub.
- Não compartilhe `DB_PASSWORD` em prints, commits ou mensagens.
- Use senhas diferentes dos valores de exemplo em ambientes compartilhados.
- Não versione `data/`, `target/`, `uploads/` ou arquivos temporários.
- Antes de commitar, execute:

```powershell
git status
```
