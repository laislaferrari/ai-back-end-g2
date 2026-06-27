# Arquitetura do Back-end — MindJourney IA

## Visão geral

O back-end do MindJourney IA utiliza uma arquitetura em camadas baseada no Spring Boot.

Cada camada possui uma responsabilidade específica, reduzindo o acoplamento e facilitando a manutenção, os testes e a evolução do projeto.

A estrutura principal segue este fluxo:

```text
Front-end
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
Banco H2
```

## Estrutura de pastas

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

## Classe principal

A classe:

```text
MindJournalApplication.java
```

é responsável por inicializar a aplicação Spring Boot.

Ela utiliza a anotação:

```java
@SpringBootApplication
```

Essa anotação habilita a configuração automática do Spring e o escaneamento dos componentes presentes no pacote `com.mindjournal` e em seus subpacotes.

## Controller

A camada `controller` recebe as requisições HTTP enviadas pelo front-end.

Suas responsabilidades são:

* Definir as rotas da API.
* Receber parâmetros e corpos de requisição.
* Aplicar validações.
* Chamar os métodos da camada de service.
* Retornar respostas HTTP.

Exemplos:

```text
HealthController
SessionController
```

O controller não deve concentrar regras de negócio.

## Service

A camada `service` concentra as regras da aplicação.

Suas responsabilidades são:

* Criar sessões.
* Buscar sessões.
* Validar a existência de registros.
* Definir a ordem dos resultados.
* Converter entidades em DTOs.
* Coordenar o acesso aos repositórios.
* Lançar exceções quando necessário.

Exemplo:

```text
SessionService
```

## Repository

A camada `repository` realiza o acesso ao banco de dados.

Os repositórios utilizam o Spring Data JPA e herdam de:

```java
JpaRepository
```

Exemplos:

```text
SessionRepository
MessageRepository
```

Métodos utilizados:

```java
List<Session> findAllByOrderByUpdatedAtDesc();
```

Esse método lista as sessões da mais recentemente atualizada para a mais antiga.

```java
List<Message> findBySession_IdOrderByTimestampAsc(Long sessionId);
```

Esse método lista as mensagens da mais antiga para a mais recente.

## Entity

A camada `entity` representa as tabelas persistidas no banco de dados.

### Session

Representa uma sessão do diário.

Campos principais:

```text
id
title
createdAt
updatedAt
```

A tabela utiliza o nome:

```text
journal_sessions
```

Esse nome evita possíveis conflitos com a palavra `session` no banco de dados.

### Message

Representa uma mensagem de uma sessão.

Campos principais:

```text
id
content
role
timestamp
session
```

Cada mensagem pertence a uma sessão.

A relação utilizada é:

```java
@ManyToOne
```

Isso significa que uma sessão pode possuir várias mensagens.

### MessageRole

Enum utilizado para identificar quem enviou a mensagem.

Valores:

```text
USER
ASSISTANT
```

## DTO

A camada `dto` define os dados de entrada e saída da API.

Ela evita que as entidades do banco sejam expostas diretamente ao front-end.

DTOs atuais:

```text
CreateSessionRequest
SessionResponse
MessageResponse
```

### CreateSessionRequest

Utilizado para receber o título de uma nova sessão.

Exemplo:

```json
{
  "title": "Meu diário de hoje"
}
```

### SessionResponse

Utilizado para devolver os dados de uma sessão.

Campos:

```text
id
title
createdAt
updatedAt
```

### MessageResponse

Utilizado para devolver os dados de uma mensagem.

Campos:

```text
id
content
role
timestamp
```

## Validação

A validação dos dados recebidos utiliza Bean Validation.

Exemplo:

```java
@NotBlank
@Size(max = 150)
```

O título de uma sessão:

* Não pode ser vazio.
* Deve possuir no máximo 150 caracteres.

As validações são ativadas no controller por meio de:

```java
@Valid
```

## Tratamento de erros

O projeto utiliza uma classe global de tratamento de exceções:

```text
GlobalExceptionHandler
```

Ela utiliza:

```java
@RestControllerAdvice
```

As respostas de erro seguem o padrão `ProblemDetail` do Spring.

### Sessão inexistente

Quando uma sessão não é encontrada, a API retorna:

```text
HTTP 404
```

Exemplo:

```json
{
  "type": "about:blank",
  "title": "Sessão não encontrada",
  "status": 404,
  "detail": "Não foi encontrada uma sessão com o ID 999."
}
```

### Dados inválidos

Quando os dados enviados não passam pela validação, a API retorna:

```text
HTTP 400
```

Exemplo:

```json
{
  "type": "about:blank",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "O título da sessão é obrigatório."
}
```

## Banco de dados

Durante a primeira etapa, o projeto utiliza H2 persistido em arquivo.

URL de conexão:

```text
jdbc:h2:file:./data/mindjournal
```

Usuário:

```text
sa
```

Senha:

```text
vazia
```

O banco é armazenado localmente em:

```text
data/mindjournal.mv.db
```

A pasta `data` não deve ser versionada.

## Configuração JPA

O Hibernate está configurado com:

```yaml
ddl-auto: update
```

Isso permite que as tabelas sejam criadas ou atualizadas automaticamente a partir das entidades.

O projeto também utiliza:

```yaml
open-in-view: false
```

Essa configuração evita que sessões do banco permaneçam abertas durante toda a resposta HTTP.

## Datas e horários

As entidades utilizam:

```java
Instant
```

Isso permite armazenar datas e horários em UTC.

As datas são preenchidas automaticamente por meio de:

```java
@PrePersist
@PreUpdate
```

`createdAt` é criado no primeiro salvamento.

`updatedAt` é atualizado sempre que uma sessão é modificada.

## Fluxo de criação de sessão

```text
POST /api/sessions
        ↓
SessionController
        ↓
SessionService
        ↓
SessionRepository
        ↓
Banco H2
        ↓
SessionResponse
```

## Fluxo de listagem

```text
GET /api/sessions
        ↓
SessionController
        ↓
SessionService
        ↓
SessionRepository
        ↓
Ordenação por updatedAt
        ↓
Lista de SessionResponse
```

## Fluxo de histórico

```text
GET /api/sessions/{id}/messages
        ↓
SessionController
        ↓
SessionService
        ↓
Verificação da sessão
        ↓
MessageRepository
        ↓
Ordenação por timestamp
        ↓
Lista de MessageResponse
```

## Comunicação com o front-end

O front-end consumirá a API pela URL:

```text
http://localhost:8080
```

Principais respostas HTTP utilizadas:

```text
200 OK
201 Created
400 Bad Request
404 Not Found
```

## CORS

A aplicação possui configuração para permitir requisições do front-end durante o desenvolvimento.

Os cabeçalhos relacionados ao CORS podem aparecer nas respostas:

```text
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
```

## Upload de arquivos

O tamanho máximo está configurado no `application.yml`:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

Essa configuração impede o recebimento de arquivos maiores que o limite definido.

## Decisões arquiteturais

As principais decisões da primeira etapa foram:

* Separar front-end e back-end em repositórios diferentes.
* Utilizar Java 17.
* Utilizar Spring Boot.
* Organizar o projeto em camadas.
* Utilizar DTOs para entrada e saída.
* Centralizar regras no service.
* Utilizar repositórios JPA.
* Persistir dados inicialmente no H2.
* Padronizar erros com `ProblemDetail`.
* Utilizar Maven Wrapper.
* Desenvolver funcionalidades em branches separadas.

## Evolução futura

A arquitetura permite substituir ou adicionar componentes sem alterar toda a aplicação.

Evoluções possíveis:

* PostgreSQL no lugar do H2.
* Autenticação com Spring Security.
* Integração com API de inteligência artificial.
* Armazenamento de arquivos em serviço externo.
* Documentação com Swagger.
* Testes unitários e de integração.
* Docker.
* Deploy em ambiente de nuvem.
