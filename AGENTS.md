# AGENTS.md — MindJourney IA Back-end

## Visão geral

Este repositório contém o back-end do MindJourney IA, um diário inteligente baseado em sessões e mensagens.

A aplicação fornece uma API REST para:

* Verificar a disponibilidade do back-end.
* Criar e consultar sessões.
* Consultar o histórico de mensagens.
* Enviar mensagens ao chat.
* Receber arquivos enviados pelo usuário.
* Preparar a integração com serviços de inteligência artificial.

O front-end e o back-end são mantidos em repositórios separados.

## Tecnologias obrigatórias

* Java 17
* Spring Boot 3
* Spring Web
* Spring Data JPA
* Bean Validation
* H2 Database
* Maven
* Maven Wrapper

Não substituir essas tecnologias sem solicitação explícita.

## Execução do projeto

No PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

No Git Bash ou Linux:

```bash
./mvnw spring-boot:run
```

Compilação:

```powershell
.\mvnw.cmd clean compile
```

Testes:

```powershell
.\mvnw.cmd test
```

A aplicação utiliza a porta:

```text
8080
```

URL local:

```text
http://localhost:8080
```

## Estrutura do projeto

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

## Regras de arquitetura

A aplicação deve manter a separação em camadas.

### Controller

Responsável por:

* Definir endpoints.
* Receber parâmetros e corpos de requisição.
* Ativar validações com `@Valid`.
* Chamar a camada de service.
* Retornar respostas HTTP.

Não colocar regras de negócio no controller.

### Service

Responsável por:

* Regras de negócio.
* Validação da existência de registros.
* Coordenação entre repositórios.
* Conversão de entidades para DTOs.
* Operações transacionais.
* Lançamento de exceções da aplicação.

Utilizar:

```java
@Service
```

Para operações que alteram dados:

```java
@Transactional
```

Para consultas:

```java
@Transactional(readOnly = true)
```

### Repository

Responsável pelo acesso aos dados.

Os repositórios devem estender:

```java
JpaRepository
```

Evitar consultas manuais quando os métodos derivados do Spring Data forem suficientes.

### Entity

Responsável pelo mapeamento das tabelas.

Utilizar anotações JPA, como:

```java
@Entity
@Table
@Id
@GeneratedValue
@Column
@ManyToOne
@JoinColumn
```

Não retornar entidades diretamente nos controllers.

### DTO

Utilizar DTOs para entrada e saída da API.

Preferir `record` para DTOs simples e imutáveis.

Exemplo:

```java
public record SessionResponse(
    Long id,
    String title
) {
}
```

### Exception

Centralizar o tratamento de erros em:

```text
GlobalExceptionHandler
```

Utilizar:

```java
@RestControllerAdvice
```

As respostas devem seguir o padrão:

```java
ProblemDetail
```

## Pacote principal

Todas as classes da aplicação devem permanecer dentro do pacote:

```text
com.mindjournal
```

Exemplos:

```java
package com.mindjournal.controller;
package com.mindjournal.service;
package com.mindjournal.repository;
```

Não criar classes da aplicação fora desse pacote, pois elas podem não ser encontradas pelo escaneamento do Spring Boot.

## Entidades atuais

### Session

Representa uma sessão do diário.

Campos:

```text
id
title
createdAt
updatedAt
```

Tabela:

```text
journal_sessions
```

Não renomear a tabela para `session`, pois a palavra pode causar conflitos em bancos de dados.

### Message

Representa uma mensagem pertencente a uma sessão.

Campos:

```text
id
content
role
timestamp
session
```

Relacionamento:

```java
@ManyToOne
```

### MessageRole

Valores permitidos:

```text
USER
ASSISTANT
```

## Datas e horários

Utilizar:

```java
Instant
```

As datas devem ser armazenadas em UTC.

Utilizar:

```java
@PrePersist
@PreUpdate
```

quando o preenchimento automático fizer parte da entidade.

## Endpoints atuais

### Health

```http
GET /api/health
```

### Sessões

```http
POST /api/sessions
GET /api/sessions
GET /api/sessions/{id}
GET /api/sessions/{id}/messages
```

### Chat

```http
POST /api/chat
```

### Upload

```http
POST /api/upload
```

Antes de alterar um endpoint, verificar se o front-end já depende do formato atual.

## Respostas HTTP

Utilizar os códigos adequados:

```text
200 OK
201 Created
400 Bad Request
404 Not Found
500 Internal Server Error
```

Para criação bem-sucedida:

```java
HttpStatus.CREATED
```

Para consultas bem-sucedidas:

```java
ResponseEntity.ok(...)
```

## Validações

Utilizar Bean Validation.

Exemplos:

```java
@NotBlank
@Size
@NotNull
```

As mensagens de validação devem ser claras e escritas em português.

Exemplo:

```java
@NotBlank(message = "O título da sessão é obrigatório.")
```

## Banco de dados

O projeto utiliza H2 persistido em arquivo.

URL:

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

O banco local é armazenado em:

```text
data/mindjournal.mv.db
```

A pasta `data` não deve ser versionada.

Se o H2 retornar erro de usuário ou senha durante o desenvolvimento e não houver dados importantes, os arquivos locais dentro da pasta `data` podem ser removidos para que o banco seja recriado.

## Arquivos que não devem ser versionados

O `.gitignore` deve ignorar:

```text
target/
data/
.idea/
.vscode/
*.log
```

Não adicionar ao repositório:

* Banco H2 local.
* Arquivos compilados.
* Logs.
* Credenciais.
* Tokens.
* Chaves de API.
* Arquivos temporários do editor.

## Upload de arquivos

O limite atual é:

```text
10 MB
```

Configuração esperada:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

O upload deve validar:

* Arquivo presente.
* Tamanho máximo.
* Tipo de arquivo permitido.
* Nome do arquivo.
* Erros de processamento.

Não confiar apenas na extensão enviada pelo usuário.

## Segurança

Nunca incluir diretamente no código:

* Tokens.
* Chaves de API.
* Senhas.
* Credenciais de banco de produção.
* Segredos de serviços externos.

Utilizar variáveis de ambiente para dados sensíveis.

Exemplo:

```yaml
ai:
  api-key: ${AI_API_KEY:}
```

Não registrar conteúdos sensíveis em logs.

## Estilo de código

Manter o código simples e direto.

Regras:

* Utilizar nomes claros.
* Evitar métodos muito longos.
* Evitar duplicação.
* Evitar lógica desnecessariamente complexa.
* Manter uma responsabilidade principal por classe.
* Utilizar injeção por construtor.
* Não utilizar injeção direta em atributos com `@Autowired`.
* Remover imports não utilizados.
* Não criar abstrações sem necessidade real.
* Preservar a estrutura existente sempre que possível.

## Injeção de dependência

Utilizar construtor:

```java
public SessionService(
    SessionRepository sessionRepository,
    MessageRepository messageRepository
) {
    this.sessionRepository = sessionRepository;
    this.messageRepository = messageRepository;
}
```

Não utilizar:

```java
@Autowired
private SessionRepository sessionRepository;
```

## Ordenação dos resultados

Sessões:

```text
updatedAt em ordem decrescente
```

Método esperado:

```java
findAllByOrderByUpdatedAtDesc()
```

Mensagens:

```text
timestamp em ordem crescente
```

Método esperado:

```java
findBySession_IdOrderByTimestampAsc(...)
```

## Testes manuais

Antes de considerar uma funcionalidade concluída, testar:

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

### Criar sessão

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

### Buscar sessão

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/api/sessions/1" `
    -Method Get
```

### Histórico

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:8080/api/sessions/1/messages" `
    -Method Get
```

Também testar:

* ID inexistente.
* Campo obrigatório vazio.
* JSON inválido.
* Arquivo acima do limite.
* Arquivo ausente.
* Erros do serviço de inteligência artificial.

## Maven Wrapper

O projeto possui:

```text
mvnw
mvnw.cmd
.mvn/
```

No Windows, utilizar:

```powershell
.\mvnw.cmd
```

Não exigir que os integrantes instalem Maven globalmente.

## Branches

Cada funcionalidade deve ser desenvolvida em uma branch separada.

Branches da primeira entrega:

```text
feature/setup-backend
feature/health-endpoint
feature/session-history
feature/chat-api
feature/upload-files
feature/docs-backend
```

Fluxo esperado:

```text
main
  ↓
feature/nome-da-funcionalidade
  ↓
commit
  ↓
push
  ↓
Pull Request
  ↓
main
```

Não desenvolver diretamente na `main`.

## Commits

Utilizar mensagens claras e curtas.

Exemplos:

```text
feat: add health endpoint
feat: add session history endpoints
feat: add chat endpoint
feat: add file upload
fix: handle session not found
docs: update backend documentation
chore: add Maven Wrapper
```

## Pull Requests

Todo Pull Request deve informar:

* O que foi implementado.
* Endpoints adicionados ou modificados.
* Testes realizados.
* Limitações conhecidas.
* Arquivos de configuração alterados.

Antes de abrir um Pull Request:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
git status
```

O projeto deve compilar sem erros.

## Divisão da primeira entrega

### Laís

Responsável por:

```text
feature/setup-backend
feature/health-endpoint
feature/session-history
feature/docs-backend
```

### Mariana

Responsável por:

```text
feature/chat-api
feature/upload-files
```

Ao alterar arquivos criados por outra integrante, preservar a estrutura existente e limitar as mudanças ao necessário.

## Definição de pronto

Uma tarefa está concluída quando:

* O código compila.
* A aplicação inicia.
* O endpoint responde corretamente.
* Os erros esperados foram testados.
* Não há credenciais no código.
* O banco local não foi versionado.
* A documentação foi atualizada.
* O commit foi criado.
* A branch foi enviada.
* O Pull Request foi aberto.

## Orientações para agentes de IA

Ao trabalhar neste repositório:

1. Ler este arquivo antes de alterar o código.
2. Analisar os arquivos existentes antes de criar novos.
3. Preservar nomes, pacotes e estrutura do projeto.
4. Não reescrever arquivos sem necessidade.
5. Realizar alterações pequenas e rastreáveis.
6. Não adicionar dependências sem justificativa.
7. Não alterar contratos de endpoints silenciosamente.
8. Não expor entidades diretamente.
9. Não inserir credenciais.
10. Compilar o projeto após mudanças em Java.
11. Informar claramente os arquivos alterados.
12. Informar os comandos de teste.
13. Considerar compatibilidade com Windows e PowerShell.
14. Utilizar o Maven Wrapper.
15. Não modificar a `main` diretamente.
