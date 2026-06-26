# AGENTS.md — MindJournal AI Back-end

## 1. Projeto

**Nome:** MindJournal AI
**Repositório:** Back-end
**Stack:** Java 17, Spring Boot 3 e Maven

O MindJournal AI é uma aplicação de diário pessoal inteligente. O back-end fornece uma API REST para gerenciamento de sessões, mensagens, histórico de conversas, upload de arquivos TXT e PDF e monitoramento da integridade da aplicação.

Nesta primeira etapa, as respostas do assistente são controladas e simuladas. A integração real com modelos de Inteligência Artificial está fora do escopo atual.

## 2. Fonte da verdade

Antes de criar, alterar ou remover qualquer arquivo, o agente deve ler integralmente:

* `docs/specs/01-backend-architecture.md`
* `docs/specs/02-frontend-architecture.md`
* `docs/plans/01-backend-plan.md`
* `docs/plans/02-frontend-plan.md`

O documento `docs/specs/01-backend-architecture.md` é a fonte principal para decisões arquiteturais e contratos internos do back-end.

O documento `docs/specs/02-frontend-architecture.md` deve ser consultado para garantir que os contratos da API atendam às necessidades do front-end.

O documento `docs/plans/01-backend-plan.md` determina a ordem incremental de implementação do back-end.

O documento `docs/plans/02-frontend-plan.md` deve ser consultado para compreender dependências e pontos de integração com o front-end.

Em caso de conflito entre uma solicitação, os planos e as especificações, as especificações arquiteturais prevalecem. O agente não deve alterar silenciosamente a arquitetura ou os contratos. Deve apontar o conflito e aguardar validação da equipe.

## 3. Metodologia

O projeto segue:

* Spec-Driven Development;
* Clean Code;
* SOLID, quando aplicável;
* isolamento de responsabilidades;
* desenvolvimento orientado pelos System Docs;
* prompts estruturados pelo padrão CRISP.

O código deve ser consequência da especificação aprovada, e não o contrário.

## 4. Escopo permitido ao agente

O agente pode:

* gerar a fundação estrutural do projeto Spring Boot;
* criar classes, interfaces e configurações previstas nos System Docs;
* implementar Controllers, Services, Repositories, DTOs e entidades;
* implementar contratos da API já aprovados;
* implementar persistência em H2 no modo arquivo;
* implementar tratamento padronizado de erros com `ProblemDetail`;
* implementar respostas simuladas do assistente;
* implementar o endpoint `GET /api/health`;
* implementar upload de um arquivo TXT ou PDF por requisição;
* revisar código para garantir compatibilidade com a arquitetura documentada;
* atualizar documentação quando solicitado explicitamente.

## 5. Fora do escopo

O agente não deve implementar nesta etapa:

* autenticação ou autorização;
* integração real com OpenAI, Groq, Claude ou outro modelo;
* PostgreSQL;
* WebSocket ou Server-Sent Events;
* armazenamento de arquivos em nuvem;
* edição ou exclusão de mensagens;
* exclusão de sessões;
* busca textual;
* notificações;
* Docker, CI/CD ou deploy;
* funcionalidades não documentadas nos System Docs.

## 6. Regras arquiteturais obrigatórias

### Controllers

* São exclusivamente fronteiras HTTP.
* Recebem parâmetros e payloads.
* Convertem entradas específicas da web quando necessário.
* Delegam o processamento aos Services.
* Retornam DTOs e códigos HTTP.
* Não contêm regras de negócio.
* Não validam existência de sessão.
* Não validam extensão, tipo MIME ou tamanho de arquivos.

### Services

* Concentram a lógica da aplicação.
* Validam regras de negócio.
* Orquestram entidades e Repositories.
* Não dependem de `HttpServletRequest`, `HttpServletResponse` ou outros objetos HTTP.
* Não devem receber diretamente `MultipartFile`.
* Devem permanecer independentes do formato de transporte da web.

### Repositories

* São responsáveis apenas pelo acesso aos dados.
* Não contêm regras de negócio.
* Devem fornecer consultas compatíveis com as ordenações definidas nos System Docs.

### DTOs

* Representam contratos de entrada e saída da API.
* Devem ser imutáveis sempre que possível.
* Não devem conter regras de negócio.
* Podem utilizar `record` do Java 17.

### Entidades

* Representam os dados persistidos.
* Podem utilizar anotações JPA.
* Não devem possuir responsabilidades HTTP.
* Não devem realizar orquestração da aplicação.

## 7. Contratos obrigatórios

O agente deve preservar os seguintes endpoints:

```text
GET  /api/health
POST /api/sessions
GET  /api/sessions
GET  /api/sessions/{id}
POST /api/chat
GET  /api/sessions/{id}/messages
POST /api/upload
```

O endpoint abaixo é opcional na primeira etapa:

```text
GET /api/sessions/{id}/attachments
```

Nenhuma rota pode ser renomeada sem validação da equipe.

## 8. Regras do domínio

* Uma sessão possui várias mensagens.
* Uma sessão pode possuir vários anexos.
* Cada mensagem pertence a uma sessão.
* Cada anexo pertence a uma sessão.
* Os remetentes permitidos são `USER` e `ASSISTANT`.
* O conteúdo da mensagem não pode estar vazio.
* O upload exige `sessionId`.
* Apenas um arquivo pode ser enviado por requisição.
* Apenas arquivos `.txt` e `.pdf` são permitidos.
* O tamanho máximo é 10 MB.
* As mensagens são ordenadas por timestamp crescente.
* As sessões são ordenadas por `updatedAt` decrescente.
* Datas e horários utilizam `Instant` em ISO 8601 UTC.

## 9. Persistência

Nesta etapa:

* utilizar H2 em modo arquivo;
* utilizar a URL `jdbc:h2:file:./data/mindjournal`;
* utilizar JPA/Hibernate;
* utilizar `ddl-auto: update`;
* não utilizar `schema.sql`;
* não implementar PostgreSQL.

## 10. Tratamento de erros

Os erros devem ser retornados utilizando `ProblemDetail`.

Campos principais:

```text
status
title
detail
```

Códigos esperados:

```text
400 — requisição inválida
404 — sessão não encontrada
413 — arquivo maior que 10 MB
500 — erro interno inesperado
```

## 11. Processo obrigatório antes de gerar código

Antes de criar ou alterar arquivos, o agente deve:

1. Ler este `AGENTS.md`.
2. Ler os dois System Docs.
3. Identificar o escopo exato da tarefa.
4. Listar os arquivos que serão criados ou alterados.
5. Explicar a responsabilidade de cada arquivo.
6. Apontar qualquer conflito com a especificação.
7. Aguardar aprovação do plano antes de implementar.

## 12. Processo após implementação

Depois de gerar ou alterar código, o agente deve informar:

* arquivos criados;
* arquivos alterados;
* responsabilidade de cada arquivo;
* endpoints afetados;
* decisões tomadas;
* testes manuais recomendados;
* qualquer divergência ou suposição utilizada.

## 13. Prompt-base CRISP

### Contexto

Estamos desenvolvendo o back-end do MindJournal AI utilizando Java 17, Spring Boot 3 e Maven. A arquitetura e os contratos estão definidos nos arquivos da pasta `docs`.

### Papel

Aja como um Arquiteto de Software Sênior especialista em Java, Spring Boot, APIs REST, Clean Code, SOLID e isolamento de domínio.

### Intenção

Implemente exclusivamente o módulo solicitado, respeitando os documentos aprovados e sem alterar contratos ou responsabilidades.

### Restrições

* Não coloque regras de negócio nos Controllers.
* Não acesse banco diretamente pelos Controllers.
* Não utilize objetos HTTP nos Services.
* Não receba `MultipartFile` diretamente nos Services.
* Não crie funcionalidades fora do escopo.
* Não modifique contratos sem autorização.
* Não exponha chaves, senhas ou informações sensíveis.

### Parâmetros

Antes do código, apresente o plano e os arquivos afetados. Depois da aprovação, gere apenas os arquivos necessários e explique as alterações realizadas.
