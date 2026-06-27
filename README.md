# MindJourney IA — Back-end

API back-end do MindJourney IA, um diário inteligente que permite registrar conversas, organizar sessões e consultar o histórico de mensagens.

## Tecnologias

* Java 17
* Spring Boot 3
* Spring Web
* Spring Data JPA
* Bean Validation
* H2 Database (desenvolvimento)
* PostgreSQL + pgvector (pipeline RAG)
* Flyway (migrações)
* Testcontainers
* Maven Wrapper

## Pré-requisitos

* Java 17 ou superior
* Git
* Docker Desktop (para PostgreSQL)

Não é necessário instalar o Maven globalmente, pois o projeto possui Maven Wrapper.

```powershell
java -version
```

## Clonar o repositório

```powershell
git clone https://github.com/laislaferrari/ai-back-end-g2.git
cd ai-back-end-g2
```

## Executar com H2 (padrão — sem Docker)

```powershell
.\mvnw.cmd spring-boot:run
```

A API será iniciada em `http://localhost:8080`.

O console do H2 fica disponível em `http://localhost:8080/h2-console`.

```
JDBC URL: jdbc:h2:file:./data/mindjournal
User Name: sa
Password: (vazio)
```

A pasta `data/` contém os arquivos do banco H2 e não deve ser versionada.

## Configuração do PostgreSQL

### 1. Criar o arquivo `.env`

```powershell
copy .env.example .env
```

Edite o arquivo `.env` com os valores desejados:

```env
POSTGRES_DB=mindjournal
DB_URL=jdbc:postgresql://localhost:5433/mindjournal
DB_USERNAME=mindjournal
DB_PASSWORD=troque_esta_senha
SPRING_PROFILES_ACTIVE=postgres
```

O `.env` não deve ser versionado.

### 2. Iniciar o PostgreSQL com Docker Compose

```powershell
docker compose up -d
```

Isso inicia um container PostgreSQL 17 com a extensão pgvector. A porta `5433` do host é mapeada para a porta `5432` do container.

Para verificar se o banco está pronto:

```powershell
docker compose ps
```

### 3. Carregar as variáveis do `.env` no PowerShell

O Spring Boot não lê o arquivo `.env` automaticamente. Carregue as variáveis manualmente:

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

### 4. Executar o backend com PostgreSQL

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

As migrações do Flyway serão executadas automaticamente ao iniciar.

### 5. Testar o endpoint health

```powershell
curl.exe http://localhost:8080/api/health
```

Resposta esperada:

```json
{"status":"UP","timestamp":"2026-06-27T00:00:00Z"}
```

### 6. Executar os testes

```powershell
.\mvnw.cmd clean test
```

### 7. Parar e remover os containers

```powershell
docker compose down
```

Para parar e remover também os dados:

```powershell
docker compose down -v
```

## Estrutura principal

```
src/main/java/com/mindjournal/
├── controller/
├── dto/
├── entity/
├── exception/
├── repository/
├── service/
└── MindJournalApplication.java

src/main/resources/
├── application.yml
├── application-postgres.yml
├── db/
│   └── migration/
│       └── V1__enable_pgvector.sql
```

## Endpoints

### Health

```
GET /api/health
```

### Sessões

```
POST /api/sessions          Criar sessão
GET  /api/sessions          Listar sessões
GET  /api/sessions/{id}     Buscar sessão
GET  /api/sessions/{id}/messages  Mensagens da sessão
```

### Chat

```
POST /api/chat
```

### Upload

```
POST /api/upload
```

## Limite de upload

O limite máximo é de 10 MB por arquivo.
