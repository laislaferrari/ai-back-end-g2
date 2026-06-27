# Plano do Back-end — MindJourney IA

## Objetivo

O back-end do MindJourney IA será responsável por receber as requisições do front-end, organizar sessões de diário, armazenar mensagens, processar arquivos e disponibilizar a integração com a inteligência artificial.

A primeira entrega tem como foco criar uma fundação simples, organizada e funcional para permitir a evolução do projeto nas próximas etapas.

## Tecnologias utilizadas

* Java 17
* Spring Boot 3
* Spring Web
* Spring Data JPA
* Bean Validation
* H2 Database
* Maven
* Maven Wrapper

## Responsabilidades principais

O back-end deverá:

* Disponibilizar um endpoint para verificar se a API está ativa.
* Criar sessões de diário.
* Listar as sessões existentes.
* Buscar uma sessão específica.
* Consultar o histórico de mensagens de uma sessão.
* Receber mensagens enviadas pelo usuário.
* Preparar a integração com a inteligência artificial.
* Receber arquivos enviados pelo front-end.
* Validar os dados recebidos.
* Retornar erros padronizados.
* Persistir os dados no banco H2 durante a primeira etapa.

## Endpoints planejados

### Health

```http
GET /api/health
```

Verifica se a aplicação está ativa.

### Sessões

```http
POST /api/sessions
```

Cria uma nova sessão.

```http
GET /api/sessions
```

Lista todas as sessões.

```http
GET /api/sessions/{id}
```

Busca uma sessão específica pelo ID.

```http
GET /api/sessions/{id}/messages
```

Consulta as mensagens pertencentes a uma sessão.

### Chat

```http
POST /api/chat
```

Recebe uma mensagem do usuário, identifica a sessão relacionada e prepara a comunicação com o serviço de inteligência artificial.

### Upload

```http
POST /api/upload
```

Recebe arquivos enviados pelo usuário para processamento.

O limite configurado para upload é de 10 MB.

## Divisão das tarefas

### Laís

* `feature/setup-backend`
* `feature/health-endpoint`
* `feature/session-history`
* `feature/docs-backend`

Responsabilidades:

* Configuração inicial do Spring Boot.
* Configuração do Maven e Maven Wrapper.
* Configuração do banco H2.
* Implementação do endpoint de health.
* Implementação de sessões e histórico.
* Tratamento de erros.
* Documentação do back-end.

### Mariana

* `feature/chat-api`
* `feature/upload-files`

Responsabilidades:

* Implementação do endpoint de chat.
* Integração inicial com a inteligência artificial.
* Persistência das mensagens do usuário e da IA.
* Implementação do upload de arquivos.
* Validação de tamanho e tipo de arquivo.

## Ordem de implementação

1. Configuração do projeto Spring Boot.
2. Configuração do banco H2.
3. Criação do endpoint de health.
4. Criação das entidades de sessão e mensagem.
5. Criação dos repositórios.
6. Criação dos DTOs.
7. Criação dos services.
8. Criação dos controllers.
9. Implementação do histórico.
10. Implementação do chat.
11. Implementação do upload.
12. Testes manuais dos endpoints.
13. Atualização da documentação.

## Critérios de conclusão

A primeira entrega será considerada concluída quando:

* O projeto compilar sem erros.
* A aplicação iniciar na porta 8080.
* O endpoint de health retornar status 200.
* Uma sessão puder ser criada.
* As sessões puderem ser listadas.
* Uma sessão puder ser consultada pelo ID.
* O histórico de mensagens puder ser consultado.
* Sessões inexistentes retornarem status 404.
* Dados inválidos retornarem status 400.
* O endpoint de chat estiver disponível.
* O endpoint de upload estiver disponível.
* O limite de upload estiver configurado.
* O Maven Wrapper estiver disponível no repositório.
* A documentação estiver atualizada.

## Estratégia de versionamento

Cada funcionalidade deve ser desenvolvida em uma branch separada.

Exemplos:

```text
feature/setup-backend
feature/health-endpoint
feature/session-history
feature/chat-api
feature/upload-files
feature/docs-backend
```

Depois de concluída, cada branch deve ser enviada ao GitHub e integrada à `main` por meio de Pull Request.

## Próximas evoluções

Após a primeira entrega, o projeto poderá evoluir com:

* Integração real com um modelo de inteligência artificial.
* Autenticação de usuários.
* Relacionamento das sessões com usuários.
* Banco de dados PostgreSQL.
* Exclusão e edição de sessões.
* Geração automática de títulos.
* Resumo de sessões.
* Identificação de sentimentos.
* Busca no histórico.
* Armazenamento de arquivos.
* Testes automatizados.
* Documentação com Swagger/OpenAPI.
