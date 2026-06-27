# MindJourney IA — Back-end

API back-end do projeto MindJourney IA, um diário inteligente que permite registrar conversas, organizar sessões e consultar o histórico de mensagens.

O projeto foi desenvolvido em Java com Spring Boot e utiliza banco de dados H2 durante a primeira etapa.

## Tecnologias

* Java 17
* Spring Boot 3
* Spring Web
* Spring Data JPA
* Bean Validation
* H2 Database
* Maven
* Maven Wrapper

## Pré-requisitos

Para executar o projeto, é necessário ter instalado:

* Java 17 ou superior
* Git

Não é necessário instalar o Maven globalmente, pois o projeto possui Maven Wrapper.

Para verificar a versão do Java:

```powershell
java -version
```

## Clonar o repositório

```powershell
git clone https://github.com/laislaferrari/ai-back-end-g2.git
cd ai-back-end-g2
```

## Executar o projeto

No PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

No Git Bash ou Linux:

```bash
./mvnw spring-boot:run
```

A API será iniciada em:

```text
http://localhost:8080
```

## Compilar o projeto

```powershell
.\mvnw.cmd clean compile
```

## Executar os testes

```powershell
.\mvnw.cmd test
```

## Banco de dados H2

O projeto utiliza um banco H2 persistido localmente.

Arquivo do banco:

```text
data/mindjournal.mv.db
```

Console do H2:

```text
http://localhost:8080/h2-console
```

Dados de conexão:

```text
JDBC URL: jdbc:h2:file:./data/mindjournal
User Name: sa
Password:
```

O campo de senha deve permanecer vazio.

A pasta `data` contém arquivos locais do banco e não deve ser enviada ao GitHub.

## Limite de arquivos

O limite configurado para upload é de:

```text
10 MB por arquivo
```

## Endpoints

### Verificar o funcionamento da API

```http
GET /api/health
```

Exemplo de resposta:

```json
{
  "status": "UP",
  "timestamp": "2026-06-27T02:50:00Z"
}
```

### Criar uma sessão

```http
POST /api/sessions
```

Corpo da requisição:

```json
{
  "title": "Meu diário de hoje"
}
```

Resposta:

```json
{
  "id": 1,
  "title": "Meu diário de hoje",
  "createdAt": "2026-06-27T02:50:00Z",
  "updatedAt": "2026-06-27T02:50:00Z"
}
```

### Listar sessões

```http
GET /api/sessions
```

As sessões são retornadas da mais recentemente atualizada para a mais antiga.

### Buscar uma sessão

```http
GET /api/sessions/{id}
```

Exemplo:

```http
GET /api/sessions/1
```

### Consultar mensagens de uma sessão

```http
GET /api/sessions/{id}/messages
```

As mensagens são retornadas da mais antiga para a mais recente.

Enquanto a sessão ainda não possuir mensagens, o endpoint retorna:

```json
[]
```

### Enviar uma mensagem ao chat

```http
POST /api/chat
```

Endpoint destinado ao envio de mensagens para a inteligência artificial.

### Enviar arquivo

```http
POST /api/upload
```

Endpoint destinado ao envio de arquivos para processamento.

## Tratamento de erros

A API utiliza o padrão `ProblemDetail` do Spring.

Exemplo de sessão inexistente:

```json
{
  "type": "about:blank",
  "title": "Sessão não encontrada",
  "status": 404,
  "detail": "Não foi encontrada uma sessão com o ID 999.",
  "timestamp": "2026-06-27T02:50:00Z"
}
```

Exemplo de validação:

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "O título da sessão é obrigatório.",
  "timestamp": "2026-06-27T02:50:00Z"
}
```

## Testes manuais

### Health

```powershell
curl.exe -i http://localhost:8080/api/health
```

### Listar sessões

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/api/sessions" `
    -Method Get
```

### Criar uma sessão

```powershell
$json = @{
    title = "Meu diario de hoje"
} | ConvertTo-Json

$bodyUtf8 = [System.Text.Encoding]::UTF8.GetBytes($json)

Invoke-RestMethod `
    -Uri "http://localhost:8080/api/sessions" `
    -Method Post `
    -ContentType "application/json; charset=utf-8" `
    -Body $bodyUtf8
```

### Buscar sessão por ID

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/api/sessions/1" `
    -Method Get
```

### Consultar mensagens

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/api/sessions/1/messages" `
    -Method Get
```

## Estrutura principal

```text
src/main/java/com/mindjournal/
├── controller/
├── dto/
├── entity/
├── exception/
├── repository/
├── service/
└── MindJournalApplication.java
```

```text
src/main/resources/
└── application.yml
```

## Organização das camadas

* `controller`: recebe requisições HTTP e retorna respostas.
* `dto`: define os dados de entrada e saída da API.
* `entity`: representa as tabelas do banco.
* `exception`: centraliza erros e respostas de exceção.
* `repository`: realiza o acesso ao banco de dados.
* `service`: concentra as regras de negócio.

## Organização das branches

As funcionalidades são desenvolvidas em branches separadas.

Principais branches da primeira entrega:

```text
feature/setup-backend
feature/health-endpoint
feature/session-history
feature/chat-api
feature/upload-files
feature/docs-backend
```

Cada funcionalidade deve ser enviada para a `main` por meio de Pull Request.

## Equipe

### Laís

* Configuração do back-end
* Endpoint de health
* Sessões e histórico
* Documentação

### Mariana

* Endpoint de chat
* Upload de arquivos
