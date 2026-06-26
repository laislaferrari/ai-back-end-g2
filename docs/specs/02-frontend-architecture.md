Front-end System Docs — MindJournal AI

1. Visão geral do front-end

Aplicação SPA (Single Page Application) em React com TypeScript, Vite e Axios para o sistema de diário pessoal inteligente MindJournal AI. Interface de chat com lista de sessões, troca de mensagens e upload drag-and-drop de arquivos TXT e PDF com barra de progresso. Consome a API REST do back-end Spring Boot em http://localhost:8080/api.

2. Objetivos da experiência do usuário

Oferecer interface limpa e intuitiva tipo diário pessoal

Permitir criação e seleção rápida de sessões de conversa

Exibir histórico de mensagens com distinção visual entre usuário e assistente

Possibilitar envio de mensagens por Enter ou clique

Disponibilizar upload drag-and-drop com feedback visual de progresso

Indicar visualmente o estado de saúde da API

Fornecer feedback imediato para loading, sucesso, vazio e erro

Ser preparada para futura apresentação de respostas por IA com streaming

3. Árvore de diretórios proposta

src/
├── App.tsx
├── main.tsx
├── components/
│   ├── Sidebar.tsx
│   ├── ChatArea.tsx
│   ├── MessageBubble.tsx
│   ├── ChatInput.tsx
│   ├── UploadZone.tsx
│   ├── ProgressBar.tsx
│   └── HealthIndicator.tsx
├── hooks/
│   ├── useSessions.ts
│   ├── useMessages.ts
│   ├── useUpload.ts
│   └── useHealth.ts
├── services/
│   ├── api.ts
│   ├── sessionService.ts
│   ├── messageService.ts
│   ├── chatService.ts
│   ├── uploadService.ts
│   └── healthService.ts
├── types/
│   ├── session.ts
│   ├── message.ts
│   ├── attachment.ts
│   └── api.ts
└── utils/
    ├── formatters.ts
    └── validators.ts

4. Páginas e fluxos de navegação

A aplicação possui uma única página com layout dividido:

┌─────────────────────────────────────────┐
│  Header (título + HealthIndicator)      │
├──────────┬──────────────────────────────┤
│          │                              │
│ Sidebar  │     ChatArea                 │
│          │     ┌────────────────────┐   │
│ • Sessão1│     │ MessageBubble (usr)│   │
│ • Sessão2│     │ MessageBubble (ast)│   │
│ • Sessão3│     │ ─────────────      │   │
│          │     │ ChatInput           │   │
│ [+ Nova] │     │ [UploadZone]        │   │
│          │     └────────────────────┘   │
└──────────┴──────────────────────────────┘

Fluxo 1 — Início: Sidebar carrega lista de sessões. Se vazia, exibe placeholder "Nenhuma sessão ainda".

Fluxo 2 — Selecionar sessão: Usuário clica em sessão na Sidebar → ChatArea carrega mensagens via GET /api/sessions/{id}/messages.

Fluxo 3 — Criar sessão: Usuário clica "[+ Nova]" → POST /api/sessions → nova sessão aparece na Sidebar e é selecionada automaticamente.

Fluxo 4 — Enviar mensagem: Usuário digita no ChatInput e pressiona Enter → POST /api/chat → mensagens do usuário e da resposta aparecem no ChatArea.

Fluxo 5 — Upload: Usuário arrasta arquivo para UploadZone ou clica para selecionar → POST /api/upload (apenas com sessão ativa) → ProgressBar é exibida → confirmação ou erro.

5. Lista dos componentes React

Componente

Descrição

App

Componente raiz — compõe a interface e consome Custom Hooks. Sem estado próprio.

Sidebar

Lista de sessões com botão de nova sessão

ChatArea

Container scrollável de mensagens

MessageBubble

Mensagem individual com remetente, conteúdo e timestamp

ChatInput

Campo de texto com botão de envio

UploadZone

Área drag-and-drop que recebe o arquivo e renderiza estados

ProgressBar

Barra de progresso do upload com percentual

HealthIndicator

Indicador de status (verde/amarelo/vermelho) da API

6. Props e responsabilidades dos componentes

App

Props: nenhuma (raiz)

Responsabilidade:

Compor o layout global (Header, Sidebar, ChatArea, UploadZone)

Consumir os Custom Hooks (useSessions, useMessages, useUpload, useHealth)

Passar props para componentes filhos

Não conter regras de estado próprias

Sidebar

Prop

Tipo

Descrição

sessions

Session[]

Lista de sessões

activeSessionId

number | null

ID da sessão ativa

onSelectSession

(id: number) => void

Callback ao clicar em sessão

onCreateSession

() => void

Callback ao clicar em nova sessão

isLoading

boolean

Indicador de carregamento

ChatArea

Prop

Tipo

Descrição

messages

Message[]

Lista de mensagens

isLoading

boolean

Indicador de carregamento

MessageBubble

Prop

Tipo

Descrição

sender

Sender

Remetente (definido em types/message.ts como 'USER' | 'ASSISTANT')

content

string

Conteúdo textual

timestamp

string

Data/hora formatada (ISO 8601)

ChatInput

Prop

Tipo

Descrição

onSendMessage

(content: string) => void

Callback ao enviar

isDisabled

boolean

Desabilitado durante envio ou se API off-line

UploadZone

Prop

Tipo

Descrição

onFileSelected

(file: File) => void

Callback quando usuário seleciona/arrasta um arquivo

isUploading

boolean

Indicador de upload em andamento

error

ProblemDetail | null

Mensagem de erro do upload (status, title, detail)

isDisabled

boolean

Desabilitado quando não há sessão ativa

ProgressBar

Prop

Tipo

Descrição

percent

number

Percentual 0–100

HealthIndicator

Prop

Tipo

Descrição

status

'healthy' | 'unhealthy' | 'loading'

Estado da API

7. Lista dos Custom Hooks

Hook

Responsabilidade

useSessions

Gerenciar lista de sessões, criar nova, selecionar ativa

useMessages

Carregar histórico e enviar mensagens

useUpload

Gerenciar upload com validação, progresso e chamada HTTP

useHealth

Verificar periodicamente a saúde da API

8. Estados e comportamentos administrados por cada Hook

useSessions

Estado

Tipo

Inicial

Descrição

sessions

Session[]

[]

Lista de sessões

activeSessionId

number | null

null

Sessão selecionada

isLoading

boolean

true

Carregando lista

error

ProblemDetail | null

null

Erro ao carregar (status, title, detail)

Comportamentos:

Carregar sessões via GET /api/sessions na montagem

Criar sessão via POST /api/sessions

Selecionar sessão e definir activeSessionId

useMessages

Estado

Tipo

Inicial

Descrição

messages

Message[]

[]

Mensagens da sessão ativa

isLoading

boolean

false

Carregando histórico

isSending

boolean

false

Enviando mensagem

error

ProblemDetail | null

null

Erro ao carregar/enviar (status, title, detail)

Comportamentos:

Carregar histórico via GET /api/sessions/{id}/messages quando activeSessionId muda

Enviar mensagem via POST /api/chat e extrair userMessage e assistantMessage da resposta

Limpar mensagens quando nenhuma sessão ativa

useUpload

Estado

Tipo

Inicial

Descrição

isUploading

boolean

false

Upload em andamento

progress

number

0

Percentual 0–100

error

ProblemDetail | null

null

Erro no upload (status, title, detail)

attachment

Attachment | null

null

Anexo criado

Comportamentos:

Validar tipo (.txt, .pdf), extensão e tamanho (10 MB) do arquivo no client-side

Enviar arquivo via POST /api/upload com onUploadProgress apenas se houver sessão ativa

Atualizar progress durante o envio

Resetar estados após conclusão ou erro

Expor error com status, title e detail extraídos do ProblemDetail da API

useHealth

Estado

Tipo

Inicial

Descrição

status

'healthy' | 'unhealthy' | 'loading'

'loading'

Estado da API

Comportamentos:

Polling GET /api/health a cada 30 segundos

Atualizar status conforme resposta ou falha

9. Organização da camada de serviços HTTP

Serviço

Arquivo

Funções

api

services/api.ts

Instância do axios com baseURL: http://localhost:8080/api, timeout 30s, interceptors de erro

sessionService

services/sessionService.ts

getAll(), getById(id), create(title?)

messageService

services/messageService.ts

getBySessionId(sessionId)

chatService

services/chatService.ts

sendMessage(sessionId, content) — retorna { userMessage, assistantMessage }

uploadService

services/uploadService.ts

upload(file, sessionId, onProgress) — sessionId obrigatório

healthService

services/healthService.ts

check()

Nenhum componente chama fetch ou axios diretamente

Todos os serviços retornam dados tipados (interfaces definidas em types/)

Serviços concentram lógica de transformação de dados (ex: converter snake_case para camelCase)

Erros da API são tratados como ProblemDetail — o front-end utiliza status, title e detail

10. Fluxo de criação e seleção de sessões

App monta → useSessions carrega lista via GET /api/sessions

Sidebar exibe isLoading skeleton ou lista de sessões

Se lista vazia, Sidebar exibe "Nenhuma sessão ainda. Crie uma!"

Usuário clica "[+ Nova]" → useSessions.createSession() → POST /api/sessions → nova sessão adicionada à lista e selecionada

Usuário clica em sessão existente → useSessions.selectSession(id) → activeSessionId atualizado

11. Fluxo de envio de mensagens

Usuário digita no ChatInput e pressiona Enter (ou clica no botão enviar)

ChatInput chama onSendMessage(content) fornecido por useMessages

useMessages define isSending = true

Mensagem do usuário é adicionada otimisticamente ao array messages

chatService.sendMessage(sessionId, content) → POST /api/chat

Resposta retorna { userMessage: MessageDTO, assistantMessage: MessageDTO }

Mensagem otimista do usuário é substituída por userMessage e assistantMessage é adicionada ao array

ChatArea faz scroll automático para a última mensagem

useMessages define isSending = false

Em caso de erro, mensagem otimista é removida e error é definido

12. Fluxo de carregamento do histórico

Usuário seleciona sessão na Sidebar

useMessages detecta mudança em activeSessionId

useMessages define isLoading = true e limpa mensagens anteriores

messageService.getBySessionId(id) → GET /api/sessions/{id}/messages

Mensagens são ordenadas por timestamp e armazenadas no estado

ChatArea renderiza mensagens com scroll ao final

Se lista vazia, exibe placeholder "Nenhuma mensagem ainda. Escreva algo!"

useMessages define isLoading = false

13. Fluxo de upload drag-and-drop

Usuário arrasta arquivo sobre UploadZone ou clica para abrir seletor de arquivos

UploadZone chama onFileSelected(file) — apenas entrega o arquivo para o hook

useUpload valida tipo (.txt, .pdf), extensão e tamanho (10 MB)

Se inválido no client-side, useUpload define error sem chamar a API; UploadZone exibe o erro

Se válido, useUpload verifica se há sessão ativa (activeSessionId)

useUpload define isUploading = true, progress = 0

uploadService.upload(file, sessionId, onProgress) → POST /api/upload com axios onUploadProgress

ProgressBar exibe percentual atualizado a cada evento de progresso

Ao concluir, useUpload define attachment e isUploading = false

Em caso de erro, useUpload define error com status, title e detail do ProblemDetail

Após 3 segundos, feedback de sucesso/erro é removido automaticamente

14. Comportamento da barra de progresso

Visível apenas durante isUploading = true

Exibe percentual numérico centralizado (ex: "47%")

Largura da barra preenchida proporcional ao percentual

Animação suave de transição (CSS transition)

role="progressbar", aria-valuenow, aria-valuemin="0", aria-valuemax="100"

Cor verde quando completo, azul durante progresso, vermelho se erro

15. Estados de loading, sucesso, vazio e erro

Loading

Sidebar: skeleton de 3 itens pulsantes

ChatArea: 2–3 skeleton messages

HealthIndicator: círculo amarelo pulsante "Verificando..."

UploadZone: desabilitada durante carregamento inicial

Sucesso

Sidebar: lista normal de sessões

ChatArea: mensagens renderizadas

HealthIndicator: círculo verde "API ativa"

Mensagem de toast "Arquivo enviado com sucesso!" no upload

Vazio

Sidebar: "Nenhuma sessão ainda. Crie uma!"

ChatArea: "Nenhuma mensagem ainda. Escreva algo!"

UploadZone: "Arraste arquivos ou clique para selecionar"

Erro

Toast/Banner no topo com title e detail do ProblemDetail

HealthIndicator: círculo vermelho "API indisponível"

ChatInput desabilitado se API offline

UploadZone exibe mensagem de erro com detail do ProblemDetail

16. Requisitos básicos de acessibilidade

Todos os inputs possuem <label> ou aria-label

MessageBubble com role="article" e aria-label informando remetente

ProgressBar com role="progressbar", aria-valuenow, aria-valuemin, aria-valuemax

UploadZone com role="button" e tabindex="0", ativável por Enter e Espaço

Mensagens de erro têm role="alert"

Navegação por Tab entre todos os elementos interativos

Contraste mínimo de 4.5:1 para textos

Foco visível em todos os elementos interativos

Suporte a leitores de tela para indicar estados de carregamento (aria-busy)

17. Contratos da API consumidos pelo front-end

Método

Rota

Consumido por

Serviço

GET

/api/health

useHealth

healthService.check()

GET

/api/sessions

useSessions

sessionService.getAll()

POST

/api/sessions

useSessions

sessionService.create(title?)

GET

/api/sessions/{id}

useSessions

sessionService.getById(id)

POST

/api/chat

useMessages

chatService.sendMessage(sessionId, content) — resposta: { userMessage, assistantMessage }

GET

/api/sessions/{id}/messages

useMessages

messageService.getBySessionId(id)

POST

/api/upload

useUpload

uploadService.upload(file, sessionId, onProgress) — sessionId obrigatório

GET

/api/sessions/{id}/attachments

opcional Etapa 1

opcional Etapa 1

18. Restrições arquiteturais

Componentes React são responsáveis exclusivamente por renderização

Estado, efeitos colaterais e regras de comportamento permanecem em Custom Hooks

Chamadas HTTP são concentradas exclusivamente na camada de serviços

Nenhum componente visual realiza chamadas à API diretamente

App apenas compõe a interface e consome os Custom Hooks — não contém regras de estado próprias

UploadZone recebe o arquivo e renderiza estados; validação e chamada HTTP ficam no useUpload

Upload é permitido apenas quando existe uma sessão ativa

Erros da API seguem ProblemDetail — o front-end utiliza status, title e detail

Upload utiliza onUploadProgress do axios para progresso

Dados da API são mapeados para interfaces TypeScript em types/

A interface respeita princípios básicos de acessibilidade (WCAG 2.1 nível A)

Nenhuma biblioteca externa de UI é utilizada — componentes são próprios

19. Critérios de aceite da primeira etapa

Sidebar carrega e exibe lista de sessões

Botão "[+ Nova]" cria sessão e a seleciona automaticamente

Clique em sessão carrega histórico de mensagens

Input de texto envia mensagem e exibe resposta do assistente com userMessage e assistantMessage

ChatArea faz scroll automático para última mensagem

Upload drag-and-drop aceita .txt e .pdf e exibe progresso

Upload é permitido apenas quando há sessão ativa

Upload de tipo inválido exibe erro imediato (validação no useUpload, sem chamar API)

Upload com sessionId inválido exibe erro com detail do ProblemDetail

HealthIndicator exibe estado da API (verde/amarelo/vermelho)

Estados de loading são exibidos com skeleton ou spinner

Estados de vazio exibem placeholder amigável

Estados de erro exibem title e detail do ProblemDetail

Todos os elementos interativos são acessíveis por teclado

Nenhum componente chama API diretamente

Nenhum hook contém JSX

App não contém estado próprio — apenas consome hooks

20. Estratégia para futura apresentação das respostas de IA

Criar hook useAIResponse que gerencia conexão WebSocket ou SSE para streaming

MessageBubble deve suportar modo "streaming" com content parcial e animação de digitação

ChatArea deve scrollar automaticamente durante streaming

Componente MessageBubble expõe prop isStreaming e streamingContent para renderização incremental

Preparar tipagem em types/message.ts para incluir status: 'complete' | 'streaming' | 'error'

A troca entre resposta simulada e streaming real ocorre via configuração no hook useMessages

Decisões aprovadas pela equipe

Stack: Java 17, Spring Boot 3 e Maven no back-end.

Stack front-end: React, TypeScript, Vite e Axios.

Persistência: H2 em modo arquivo na primeira etapa (jdbc:h2:file:./data/mindjournal), garantindo dados após reinício.

PostgreSQL: Reservado como possibilidade futura, não implementado agora.

JPA: ddl-auto: update gera o schema automaticamente; schema.sql não será utilizado.

Upload: sessionId é obrigatório. O multipart contém os campos file e sessionId.

Validação de sessão: ocorre nos Services (via SessionService), nunca nos Controllers.

Validação de arquivo: tipo, extensão e tamanho validados no AttachmentService.

Controllers: apenas recebem, convertem e delegam — sem regras de negócio.

Contrato POST /api/chat: retorna userMessage e assistantMessage, ambos MessageDTO.

Datas: ISO 8601 UTC (Instant) em todas as respostas e entidades.

Ordenação de sessões: por updatedAt decrescente.

Ordenação de mensagens: por timestamp crescente.

Limite de upload: 10 MB, um arquivo por requisição.

Upload no front-end: permitido apenas quando há uma sessão ativa.

UploadZone: recebe o arquivo e renderiza estados; validação e HTTP no useUpload.

Componente App: apenas compõe a interface e consome Custom Hooks — sem estado próprio.

Erros padronizados: ProblemDetail (RFC 9457) com campos status, title e detail.

AttachmentController: recebe MultipartFile e converte para objeto de entrada independente de HTTP antes de chamar o service.

AttachmentService: não recebe MultipartFile — recebe nome, tipo MIME, tamanho, conteúdo do arquivo e sessionId.

GET /api/sessions/{id}/attachments: endpoint opcional na Etapa 1, não bloqueia requisitos mínimos.

Seção final

Suposições utilizadas na especificação

Back-end em http://localhost:8080/api

Respostas do assistente são instantâneas (sem atraso simulado)

Upload é realizado um arquivo por vez

Sessões são sempre carregadas integralmente (sem paginação na primeira etapa)

Navegador moderno com suporte a Drag and Drop API e axios

TypeScript estrito habilitado

Dados de erro da API seguem o formato ProblemDetail (RFC 9457)

Decisões que precisam de validação da equipe

As decisões abaixo foram aprovadas e registradas na seção "Decisões aprovadas pela equipe" acima.

Riscos arquiteturais identificados

Ausência de estado global (Context API ou Zustand) pode levar a prop drilling excessivo com o crescimento da aplicação

Upload via onUploadProgress do axios depende de suporte do back-end (header Content-Length)

Polling de health check a cada 30s pode gerar carga desnecessária em rede móvel

Mensagem otimista no envio pode causar inconsistência se o back-end rejeitar a mensagem

Funcionalidades consideradas fora do escopo da primeira etapa

Autenticação e login

Roteamento com React Router (múltiplas páginas)

Tema escuro / claro

Edição e exclusão de mensagens

Notificações push

PWA e instalação

Testes automatizados (serão especificados na segunda etapa)

Internacionalização (i18n)

Animações avançadas

Suporte a markdown ou rich text nas mensagens