# Prompt para geração dos System Docs — MindJournal AI

Aja como um Arquiteto de Software Sênior especialista em Spring Boot, React, APIs REST, Arquitetura de Software, Clean Code, SOLID, Spec-Driven Development e aplicações preparadas para integração com Inteligência Artificial.

## CONTEXTO

Estamos desenvolvendo o projeto acadêmico **MindJournal AI**, um diário pessoal inteligente utilizando React no front-end e Spring Boot no back-end.

Nesta etapa inicial, o foco é construir a fundação arquitetural da aplicação, definindo estruturas, responsabilidades, contratos e fluxos antes de qualquer implementação de código.

Uma sessão representa uma entrada ou conversa do diário. Cada sessão pode possuir diversas mensagens enviadas pelo usuário, respostas controladas do assistente e arquivos anexados.

O sistema deve contemplar:

* criação de sessões de diário;
* envio de mensagens em formato de chat;
* recebimento de respostas textuais controladas de teste;
* persistência das mensagens por ID de sessão;
* listagem e recuperação do histórico;
* upload de documentos nos formatos `.txt` e `.pdf`;
* registro dos metadados básicos dos anexos;
* interface drag-and-drop com progresso visual;
* endpoint de monitoramento `GET /api/health`;
* arquitetura preparada para futura integração com modelos de Inteligência Artificial.

## INTENÇÃO

Crie dois Documentos de Especificação do Sistema:

1. `backend-system-docs.md`
2. `frontend-system-docs.md`

Esses documentos serão utilizados como fonte da verdade para a implementação futura e serão adicionados aos dois repositórios do projeto.

Não escreva o código final.

O objetivo desta etapa é definir exclusivamente:

* arquitetura;
* responsabilidades;
* contratos;
* entidades;
* componentes;
* fluxos;
* regras;
* critérios de aceite.

## RESTRIÇÕES INEGOCIÁVEIS

1. Siga os princípios de Clean Code, SOLID, Spec-Driven Development e isolamento de domínio.

2. No back-end:

* Controllers devem atuar somente como fronteiras HTTP.
* Controllers não podem conter regras de negócio.
* Services devem concentrar a lógica da aplicação e a orquestração do domínio.
* Services não devem depender de conceitos HTTP.
* Repositories devem tratar exclusivamente o acesso aos dados.
* DTOs devem representar os contratos de entrada e saída da API.
* A camada de domínio não deve conhecer detalhes concretos de banco de dados ou transporte HTTP.
* O endpoint `GET /api/health` é obrigatório.
* O upload deve aceitar apenas arquivos `.txt` e `.pdf`.

3. No front-end:

* Componentes React devem ser responsáveis principalmente pela renderização.
* Estado, efeitos colaterais e regras de comportamento devem permanecer em Custom Hooks.
* Chamadas HTTP devem ser concentradas em uma camada de serviços.
* Componentes visuais não devem realizar diretamente chamadas à API.
* A interface deve respeitar princípios básicos de acessibilidade.
* O upload deve oferecer drag-and-drop e progresso visual.

4. A arquitetura deve permitir a futura substituição das respostas controladas por uma integração real com IA sem alterar o núcleo do domínio.

5. Não utilize bibliotecas externas dentro da camada de domínio.

6. Não gere código nesta etapa.

7. Não crie funcionalidades além do escopo sem apresentá-las primeiro como sugestão opcional.

## PARÂMETROS DE SAÍDA

Entregue a resposta em Markdown profissional e divida claramente o resultado nos dois documentos abaixo.

### Documento 1 — `backend-system-docs.md`

Deve conter:

1. Visão geral do back-end.
2. Objetivos da API.
3. Árvore de diretórios proposta.
4. Responsabilidade de cada camada.
5. Modelo conceitual das entidades:

   * Session;
   * Message;
   * Attachment.
6. Relacionamentos entre as entidades.
7. DTOs previstos.
8. Controllers previstos.
9. Services previstos.
10. Repositories previstos.
11. Tabela dos contratos da API REST contendo:

    * método HTTP;
    * rota;
    * finalidade;
    * parâmetros;
    * payload de entrada;
    * payload de saída;
    * códigos de resposta.
12. Fluxo de criação de sessão.
13. Fluxo de envio de mensagem.
14. Fluxo de recuperação do histórico.
15. Fluxo de upload de documentos.
16. Estratégia de persistência relacional.
17. Validações e tratamento de erros.
18. Restrições arquiteturais.
19. Critérios de aceite da primeira etapa.
20. Estratégia para futura integração com IA.

### Documento 2 — `frontend-system-docs.md`

Deve conter:

1. Visão geral do front-end.
2. Objetivos da experiência do usuário.
3. Árvore de diretórios proposta.
4. Páginas e fluxos de navegação.
5. Lista dos componentes React.
6. Props e responsabilidades dos componentes.
7. Lista dos Custom Hooks.
8. Estados e comportamentos administrados por cada Hook.
9. Organização da camada de serviços HTTP.
10. Fluxo de criação e seleção de sessões.
11. Fluxo de envio de mensagens.
12. Fluxo de carregamento do histórico.
13. Fluxo de upload drag-and-drop.
14. Comportamento da barra de progresso.
15. Estados de loading, sucesso, vazio e erro.
16. Requisitos básicos de acessibilidade.
17. Contratos da API consumidos pelo front-end.
18. Restrições arquiteturais.
19. Critérios de aceite da primeira etapa.
20. Estratégia para futura apresentação das respostas de IA.

### Seção final obrigatória

Inclua ao final:

* suposições utilizadas na especificação;
* decisões que precisam de validação da equipe;
* riscos arquiteturais identificados;
* funcionalidades consideradas fora do escopo da primeira etapa.

Não gere código até que os dois documentos sejam analisados e aprovados pela equipe.
