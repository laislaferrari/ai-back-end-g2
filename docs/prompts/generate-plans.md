Aja como um Arquiteto de Software Sênior e Líder Técnico especialista em Spring Boot, React, TypeScript, Clean Code, SOLID, Spec-Driven Development e planejamento incremental de software.

## CONTEXTO

Estamos desenvolvendo o projeto acadêmico MindJournal AI.

As especificações arquiteturais já foram revisadas e aprovadas pela equipe e estão nos arquivos:

* `docs/specs/01-backend-architecture.md`
* `docs/specs/02-frontend-architecture.md`

O projeto utiliza:

### Back-end

* Java 17
* Spring Boot 3
* Maven
* Spring Data JPA
* H2 em modo arquivo
* API REST

### Front-end

* React
* TypeScript
* Vite
* Axios
* Custom Hooks

O projeto possui dois repositórios separados, mas os documentos de arquitetura e planejamento estarão presentes nos dois para facilitar a comunicação entre as equipes.

## INTENÇÃO

Crie dois Planos de Implementação:

1. `docs/plans/01-backend-plan.md`
2. `docs/plans/02-frontend-plan.md`

Os planos devem transformar as especificações aprovadas em uma sequência clara, incremental e executável de tarefas.

Não gere código nesta etapa.

Não altere as especificações existentes.

## RESTRIÇÕES INEGOCIÁVEIS

1. Baseie todas as decisões exclusivamente nos arquivos de arquitetura aprovados.
2. Não invente endpoints, componentes, entidades ou funcionalidades.
3. Preserve todos os contratos de comunicação entre front-end e back-end.
4. Organize a implementação em fases pequenas e progressivas.
5. Services do back-end devem ser implementados antes dos Controllers que dependem deles.
6. Componentes visuais do front-end não podem realizar chamadas HTTP diretamente.
7. Custom Hooks devem concentrar estado, efeitos e comportamentos.
8. Services do front-end devem concentrar a comunicação HTTP.
9. Controllers do back-end não podem conter regras de negócio.
10. Não inclua integração real com IA, autenticação, deploy ou funcionalidades fora da primeira etapa.
11. Não gere código-fonte.

## PARÂMETROS DE SAÍDA

### Documento 1 — `01-backend-plan.md`

Organize o plano em fases contendo:

1. Objetivo da fase.
2. Dependências da fase.
3. Arquivos ou pacotes envolvidos.
4. Tarefas de implementação.
5. Contratos da API afetados.
6. Validações necessárias.
7. Testes manuais recomendados.
8. Critérios de aceite.
9. Definition of Done.

A ordem sugerida deve considerar:

* bootstrap do Spring Boot;
* configurações e dependências;
* entidades e enums;
* DTOs;
* Repositories;
* Services;
* resposta simulada do assistente;
* tratamento de erros;
* endpoint de saúde;
* sessões;
* mensagens e chat;
* upload;
* integração final e validação.

### Documento 2 — `02-frontend-plan.md`

Organize o plano em fases contendo:

1. Objetivo da fase.
2. Dependências da fase.
3. Arquivos ou pastas envolvidos.
4. Componentes afetados.
5. Hooks afetados.
6. Serviços HTTP afetados.
7. Estados e comportamentos.
8. Testes manuais recomendados.
9. Critérios de aceite.
10. Definition of Done.

A ordem sugerida deve considerar:

* bootstrap React com Vite e TypeScript;
* estrutura de pastas;
* tipos da API;
* configuração do Axios;
* layout base;
* indicador de saúde;
* sessões e Sidebar;
* chat e histórico;
* upload drag-and-drop;
* barra de progresso;
* estados de loading, vazio e erro;
* acessibilidade;
* integração final com o back-end.

## SEÇÃO FINAL OBRIGATÓRIA

Em cada plano, inclua:

* dependências entre fases;
* tarefas que podem ser realizadas em paralelo;
* tarefas que dependem do outro repositório;
* riscos de integração;
* checklist final da primeira etapa;
* sugestão de branches por funcionalidade.

Não gere código.
