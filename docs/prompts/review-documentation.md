# Revisão final de coerência — MindJournal AI

Aja como um Arquiteto de Software Sênior especialista em Spring Boot, React, TypeScript, Clean Code, SOLID e Spec-Driven Development.

## CONTEXTO

A documentação arquitetural e os planos de implementação do MindJournal AI já foram produzidos.

Leia integralmente os seguintes arquivos:

* `docs/prompts/system-docs-crisp.md`
* `docs/prompts/generate-plans.md`
* `docs/specs/01-backend-architecture.md`
* `docs/specs/02-frontend-architecture.md`
* `docs/plans/01-backend-plan.md`
* `docs/plans/02-frontend-plan.md`

Sua tarefa é revisar e corrigir exclusivamente esses documentos.

Não gere código-fonte.
Não crie o projeto Spring Boot.
Não crie o projeto React.
Não altere o escopo funcional aprovado.

## AJUSTES OBRIGATÓRIOS

### 1. Coerência entre domínio e JPA

Substitua qualquer afirmação absoluta de que a camada de domínio não possui dependências de persistência por esta regra:

> As entidades podem utilizar anotações JPA, mas não devem possuir responsabilidades HTTP ou regras de orquestração da aplicação. As regras da aplicação e os Services não devem depender de detalhes concretos da persistência.

Preserve o uso das entidades JPA no pacote `model`.

### 2. Arquivo AttachmentType

No plano do back-end, adicione à lista de arquivos da fase de entidades:

`src/main/java/com/mindjournal/model/AttachmentType.java`

O enum deve possuir os valores:

* `TXT`
* `PDF`

Não gere o arquivo Java. Apenas documente sua criação no plano.

### 3. Métodos do SessionService

Documente no `SessionService` os seguintes métodos com responsabilidades distintas:

* `getSession(Long id)`: retorna `SessionDTO` para uso nos contratos da API.
* `requireSession(Long id)`: retorna a entidade `Session`, lança exceção quando não encontrada e é utilizado internamente por outros Services.
* `touchSession(Long id)`: atualiza `updatedAt` para o instante atual.

Registre que `requireSession` não é chamado diretamente pelos Controllers.

Documente que:

* `MessageService`;
* `ChatService`;
* `AttachmentService`;

podem utilizar `requireSession` internamente.

### 4. Atualização da última atividade

Preserve a regra de que:

* o envio bem-sucedido de uma mensagem atualiza `Session.updatedAt`;
* o upload bem-sucedido de um anexo atualiza `Session.updatedAt`.

O `ChatService` e o `AttachmentService` devem chamar `SessionService.touchSession`.

### 5. Título padrão da sessão

Preserve a seguinte regra:

* `title` é opcional apenas no `CreateSessionRequest`;
* quando estiver ausente, vazio ou apenas com espaços, o `SessionService` utiliza `"Nova sessão"`;
* o `SessionDTO` sempre retorna `title` preenchido;
* no front-end, `Session.title` permanece do tipo `string`.

### 6. Limite da mensagem

Remova qualquer observação como:

> definir limite máximo do conteúdo da mensagem

Nenhum limite adicional para a mensagem foi aprovado nesta etapa.

Preserve apenas a validação de que o conteúdo não pode estar vazio ou conter somente espaços.

### 7. Configuração do multipart

No plano do back-end, altere a configuração planejada para:

* `spring.servlet.multipart.max-file-size: 10MB`
* `spring.servlet.multipart.max-request-size: 11MB`

O arquivo continua limitado a 10 MB. O limite maior da requisição deve acomodar os dados adicionais do multipart.

### 8. Terminologia das sessões

Substitua expressões como:

> CRUD de sessões

por:

> operações de criação, listagem e busca de sessões

Não estão previstos endpoints de edição ou exclusão na primeira etapa.

### 9. Remover código executável dos planos

Os planos não devem conter implementações completas prontas para copiar.

Converta blocos de Java, TypeScript e configuração executável em descrições de contratos.

Exemplo:

Em vez de um bloco TypeScript completo, utilize:

`Session`

* `id`: number
* `title`: string
* `createdAt`: string em ISO 8601
* `updatedAt`: string em ISO 8601

Assinaturas curtas de métodos podem permanecer para explicar contratos, mas não gere corpos de funções, classes completas ou implementações finais.

### 10. Sender

Padronize o remetente em toda a documentação como:

* `USER`
* `ASSISTANT`

O componente `MessageBubble` deve receber:

`sender: Sender`

O tipo `Sender` deve representar `'USER' | 'ASSISTANT'`.

### 11. ProblemDetail

Padronize os erros dos seguintes Hooks como:

`ProblemDetail | null`

* `useSessions`
* `useMessages`
* `useUpload`

O tipo possui:

* `status`
* `title`
* `detail`

Erros criados pelo próprio front-end também devem usar esse formato.

Para arquivo inválido no client-side, documente o seguinte padrão:

* `status`: 400
* `title`: `"Arquivo inválido"`
* `detail`: descrição específica da validação

### 12. Atualização da Sidebar

Adicione ao `useSessions` o comportamento:

`refreshSessions()`

Esse comportamento deve buscar novamente `GET /api/sessions`.

Registre que `refreshSessions()` será chamado depois de:

* envio bem-sucedido de uma mensagem;
* upload bem-sucedido de um anexo.

O objetivo é atualizar a ordenação da Sidebar por `updatedAt` decrescente.

O `App` pode conectar os callbacks dos Hooks, mas não deve possuir estado próprio nem regras de negócio.

### 13. Formatação Markdown

Reformate os seguintes arquivos como Markdown profissional:

* `docs/prompts/system-docs-crisp.md`
* `docs/specs/01-backend-architecture.md`
* `docs/specs/02-frontend-architecture.md`

Utilize:

* `#`, `##` e `###` para títulos;
* listas Markdown;
* tabelas com pipes;
* blocos de código cercados por crases;
* checklists com `- [ ]`.

Não resuma e não remova conteúdo arquitetural aprovado.

### 14. Preservações obrigatórias

Não altere:

* nomes das entidades;
* nomes dos componentes;
* nomes dos Custom Hooks;
* rotas aprovadas;
* payloads aprovados;
* Java 17;
* Spring Boot 3;
* Maven;
* React;
* TypeScript;
* Vite;
* Axios;
* H2 em modo arquivo;
* limite de arquivo de 10 MB;
* uso de `ProblemDetail`;
* separação entre Controllers, Services e Repositories;
* separação entre componentes, Hooks e Services HTTP;
* funcionalidades fora do escopo.

## PROCESSO

Antes de editar:

1. Liste os arquivos que serão modificados.
2. Explique brevemente quais seções serão ajustadas.
3. Confirme que nenhum código-fonte será criado.
4. Aguarde aprovação.

Depois da aprovação:

1. Edite apenas os seis documentos informados.
2. Não crie arquivos da aplicação.
3. Não gere Java, TypeScript, CSS ou configurações executáveis.
4. Ao finalizar, apresente:

   * arquivos alterados;
   * ajustes realizados;
   * possíveis pendências restantes;
   * confirmação de que rotas e contratos foram preservados.
