# Plano de Implementação — Front-end MindJournal AI

## Fase 1: Bootstrap React com Vite e TypeScript

### Objetivo
Inicializar o projeto React com Vite e TypeScript, configurar dependências e estrutura de pastas.

### Dependências
- Nenhuma (fase inicial)

### Arquivos e pastas envolvidos
- `package.json` (raiz)
- `vite.config.ts`
- `tsconfig.json`
- `index.html`
- `src/main.tsx`
- `src/App.tsx`

### Componentes afetados
- Nenhum ainda

### Hooks afetados
- Nenhum ainda

### Serviços HTTP afetados
- Nenhum ainda

### Estados e comportamentos
- Nenhum ainda

### Testes manuais recomendados
- Executar `npm create vite@latest . -- --template react-ts` e confirmar `npm run dev` abre em `http://localhost:5173`

### Critérios de aceite
- [ ] `npm run dev` inicia servidor de desenvolvimento
- [ ] Página inicial renderiza "MindJournal AI" no navegador
- [ ] Estrutura de pastas criada conforme especificação

### Definition of Done
- Commit inicial com scaffold do Vite

---

## Fase 2: Estrutura de Pastas e Tipos da API

### Objetivo
Criar a estrutura completa de diretórios e definir as interfaces TypeScript que representam os contratos da API.

### Dependências
- Fase 1

### Arquivos e pastas envolvidos
- `src/components/`
- `src/hooks/`
- `src/services/`
- `src/types/session.ts`
- `src/types/message.ts`
- `src/types/attachment.ts`
- `src/types/api.ts`
- `src/utils/`

### Componentes afetados
- Nenhum ainda (pastas criadas, componentes serão implementados nas fases seguintes)

### Hooks afetados
- Nenhum ainda

### Serviços HTTP afetados
- Nenhum ainda

### Estados e comportamentos
- Nenhum ainda

### Tarefas de implementação
1. Criar estrutura de diretórios: `components/`, `hooks/`, `services/`, `types/`, `utils/`
2. Criar `types/session.ts`:
   - `Session`: `id` (number), `title` (string), `createdAt` (string ISO 8601), `updatedAt` (string ISO 8601)
3. Criar `types/message.ts`:
   - `Sender`: `'USER' | 'ASSISTANT'`
   - `Message`: `id` (number), `sessionId` (number), `sender` (Sender), `content` (string), `timestamp` (string ISO 8601)
   - `ChatResponse`: `userMessage` (Message), `assistantMessage` (Message)
4. Criar `types/attachment.ts`:
   - `AttachmentType`: `'TXT' | 'PDF'`
   - `Attachment`: `id` (number), `sessionId` (number), `filename` (string), `type` (AttachmentType), `size` (number), `uploadDate` (string ISO 8601)
5. Criar `types/api.ts`:
   - `ProblemDetail`: `status` (number), `title` (string), `detail` (string)
   - `HealthStatus`: `status` (`'UP' | 'DOWN'`), `timestamp` (string ISO 8601)

### Testes manuais recomendados
- Verificar se `npm run build` compila sem erros de tipo

### Critérios de aceite
- [ ] Interfaces TypeScript criadas para todos os contratos
- [ ] Compilação sem erros

### Definition of Done
- Commit com pastas e tipos

---

## Fase 3: Configuração do Axios e Serviços HTTP

### Objetivo
Configurar a instância do Axios e implementar todos os serviços de comunicação HTTP.

### Dependências
- Fase 2 (tipos)

### Arquivos e pastas envolvidos
- `src/services/api.ts`
- `src/services/healthService.ts`
- `src/services/sessionService.ts`
- `src/services/messageService.ts`
- `src/services/chatService.ts`
- `src/services/uploadService.ts`

### Componentes afetados
- Nenhum (serviços são consumidos pelos hooks)

### Hooks afetados
- Nenhum (serviços são consumidos pelos hooks nas fases seguintes)

### Serviços HTTP afetados
- Todos

### Estados e comportamentos
- Nenhum ainda

### Tarefas de implementação
1. Criar `api.ts`:
   - Instância do axios com `baseURL: 'http://localhost:8080/api'`
   - Timeout: 30000ms
   - Interceptor de resposta para tratar erros e extrair `ProblemDetail`
2. Criar `healthService.ts`:
   - `check(): Promise<HealthStatus>` → `GET /api/health`
3. Criar `sessionService.ts`:
   - `getAll(): Promise<Session[]>` → `GET /api/sessions`
   - `getById(id: number): Promise<Session>` → `GET /api/sessions/{id}`
   - `create(title?: string): Promise<Session>` → `POST /api/sessions`
4. Criar `messageService.ts`:
   - `getBySessionId(sessionId: number): Promise<Message[]>` → `GET /api/sessions/{id}/messages`
5. Criar `chatService.ts`:
   - `sendMessage(sessionId: number, content: string): Promise<ChatResponse>` → `POST /api/chat`
6. Criar `uploadService.ts`:
   - `upload(file: File, sessionId: number, onProgress: (percent: number) => void): Promise<Attachment>` → `POST /api/upload` com `FormData` e `onUploadProgress`
   - `sessionId` obrigatório

### Testes manuais recomendados
- Verificar importação e compilação dos serviços (teste real apenas com back-end rodando)

### Critérios de aceite
- [ ] Axios configurado com base URL e timeout
- [ ] Todos os serviços implementados com funções tipadas
- [ ] `uploadService.upload` aceita callback de progresso
- [ ] `sessionId` obrigatório em `uploadService`

### Definition of Done
- Commit com serviços HTTP

---

## Fase 4: Layout Base

### Objetivo
Criar o layout base da aplicação com a estrutura Sidebar + ChatArea + UploadZone.

### Dependências
- Fase 1 (bootstrap)

### Arquivos e pastas envolvidos
- `src/App.tsx`
- `src/components/Sidebar.tsx`
- `src/components/ChatArea.tsx`
- `src/components/ChatInput.tsx`
- `src/components/UploadZone.tsx`
- `src/components/ProgressBar.tsx`
- `src/components/HealthIndicator.tsx`
- `src/components/MessageBubble.tsx`
- `src/App.css` (estilos globais)

### Componentes afetados
- `App` (raiz, compõe layout)
- `Sidebar` (estrutura vazia ou com placeholder)
- `ChatArea` (estrutura vazia)
- `ChatInput` (input desabilitado)
- `UploadZone` (área vazia)
- `ProgressBar` (invisível por padrão)
- `HealthIndicator` (placeholder)
- `MessageBubble` (estrutura vazia)

### Hooks afetados
- Nenhum (layout estático sem estado)

### Serviços HTTP afetados
- Nenhum

### Estados e comportamentos
- Layout responsivo básico

### Tarefas de implementação
1. Criar `App.tsx` como componente raiz que compõe:
   - Header com título "MindJournal AI" + `HealthIndicator`
   - Sidebar + ChatArea em layout de duas colunas
   - UploadZone abaixo do ChatInput
2. Criar `Sidebar.tsx`:
   - Lista vazia com placeholder "Nenhuma sessão ainda"
   - Botão "[+ Nova]" desabilitado
3. Criar `ChatArea.tsx`:
   - Container scrollável vazio com placeholder "Nenhuma mensagem ainda"
4. Criar `ChatInput.tsx`:
   - Input de texto desabilitado com placeholder "Selecione uma sessão para começar"
5. Criar `UploadZone.tsx`:
   - Área com texto "Arraste arquivos ou clique para selecionar"
   - Desabilitada
6. Criar `ProgressBar.tsx`:
   - Barra invisível (0%) com estrutura HTML para `role="progressbar"`
7. Criar `HealthIndicator.tsx`:
   - Círculo amarelo com texto "Verificando..."
8. Criar `MessageBubble.tsx`:
   - Estrutura para exibir remetente, conteúdo e timestamp
9. Estilizar com CSS básico (layout flex/grid, cores, espaçamento)

### Testes manuais recomendados
- `npm run dev` e verificar layout renderizado com placeholders
- Verificar responsividade básica

### Critérios de aceite
- [ ] Layout de duas colunas (Sidebar + ChatArea) renderizado
- [ ] Todos os componentes renderizam sem erros
- [ ] Placeholders visíveis em estado vazio
- [ ] UploadZone e ChatInput desabilitados

### Definition of Done
- Commit com layout base

---

## Fase 5: Hook useHealth e Indicador de Saúde

### Objetivo
Implementar o polling de saúde da API e conectar ao `HealthIndicator`.

### Dependências
- Fase 3 (healthService)
- Fase 4 (App, HealthIndicator)

### Arquivos e pastas envolvidos
- `src/hooks/useHealth.ts`
- `src/components/HealthIndicator.tsx` (props)
- `src/App.tsx` (consumo do hook)

### Componentes afetados
- `App` (consome `useHealth` e passa props para `HealthIndicator`)
- `HealthIndicator` (recebe `status` via props)

### Hooks afetados
- `useHealth`

### Serviços HTTP afetados
- `healthService`

### Estados e comportamentos
- `status`: `'loading'` | `'healthy'` | `'unhealthy'`
- Polling a cada 30 segundos
- Loading na inicialização, healthy em 200, unhealthy em erro

### Tarefas de implementação
1. Implementar `useHealth`:
   - Estado `status` inicial `'loading'`
   - `useEffect` no mount → `check()` + `setInterval(30000)`
   - Atualiza `status` conforme resposta ou erro
   - Cleanup do intervalo no unmount
2. Atualizar `App.tsx`:
   - Consumir `useHealth`
   - Passar `status` para `HealthIndicator`
3. Atualizar `HealthIndicator`:
   - Renderizar círculo verde/amarelo/vermelho conforme status
   - Texto correspondente

### Testes manuais recomendados
- Com back-end rodando: health indicator fica verde em segundos
- Sem back-end: indicator fica vermelho após timeout

### Critérios de aceite
- [ ] `HealthIndicator` exibe estado correto da API
- [ ] Polling automático a cada 30s
- [ ] `App` consome hook sem estado próprio

### Definition of Done
- Commit com `useHealth` e integração

---

## Fase 6: Hook useSessions e Sidebar

### Objetivo
Implementar o gerenciamento de sessões com criação e listagem.

### Dependências
- Fase 3 (sessionService)
- Fase 4 (App, Sidebar)
- Fase 5 (App já consome hooks)

### Arquivos e pastas envolvidos
- `src/hooks/useSessions.ts`
- `src/components/Sidebar.tsx` (props ativas)
- `src/App.tsx` (consumo do hook)

### Componentes afetados
- `App` (consome `useSessions`, passa props para `Sidebar`)
- `Sidebar` (recebe `sessions`, `activeSessionId`, callbacks)

### Hooks afetados
- `useSessions`

### Serviços HTTP afetados
- `sessionService`

### Estados e comportamentos
- `sessions`: `Session[]`, inicial `[]`
- `activeSessionId`: `number | null`, inicial `null`
- `isLoading`: `boolean`, inicial `true`
- `error`: `ProblemDetail | null`, inicial `null`
- Carregar sessões no mount
- Criar sessão via `POST /api/sessions`
- Selecionar sessão e definir `activeSessionId`
- `refreshSessions()`: recarregar lista via `GET /api/sessions` — chamado por `App` após envio de mensagem e upload bem-sucedidos

### Tarefas de implementação
1. Implementar `useSessions`:
   - Estados: `sessions`, `activeSessionId`, `isLoading`, `error`
   - `loadSessions()` → `GET /api/sessions`, atualiza `sessions`
   - `createSession(title?)` → `POST /api/sessions`, adiciona à lista e seleciona
   - `selectSession(id)` → define `activeSessionId`
   - Chamar `loadSessions()` no mount via `useEffect`
2. Atualizar `App.tsx`:
   - Consumir `useSessions`
   - Passar props para `Sidebar`
3. Atualizar `Sidebar`:
   - Renderizar lista de sessões
   - Destacar sessão ativa
   - Botão "[+ Nova]" funcional
   - Placeholder para lista vazia
   - Skeleton para loading

### Testes manuais recomendados
- Com back-end rodando: sidebar carrega sessões
- Criar nova sessão → aparece na lista e é selecionada
- Parar back-end → estado de erro visível

### Critérios de aceite
- [ ] Sidebar exibe lista de sessões
- [ ] Botão "[+ Nova]" cria e seleciona sessão
- [ ] Sessão ativa destacada visualmente
- [ ] Estados de loading e vazio funcionais

### Definition of Done
- Commit com `useSessions` e Sidebar funcional

---

## Fase 7: Hook useMessages e Chat

### Objetivo
Implementar o carregamento do histórico e envio de mensagens.

### Dependências
- Fase 3 (messageService, chatService)
- Fase 4 (App, ChatArea, ChatInput, MessageBubble)
- Fase 6 (useSessions → activeSessionId)

### Arquivos e pastas envolvidos
- `src/hooks/useMessages.ts`
- `src/components/ChatArea.tsx` (props)
- `src/components/ChatInput.tsx` (props)
- `src/components/MessageBubble.tsx` (props)
- `src/App.tsx` (consumo do hook)

### Componentes afetados
- `App` (consome `useMessages`, passa props)
- `ChatArea` (recebe `messages`, `isLoading`)
- `ChatInput` (recebe `onSendMessage`, `isDisabled`)
- `MessageBubble` (recebe `sender: Sender`, `content`, `timestamp`)

### Hooks afetados
- `useMessages`

### Serviços HTTP afetados
- `messageService`, `chatService`

### Estados e comportamentos
- `messages`: `Message[]`, inicial `[]`
- `isLoading`: `boolean`, inicial `false`
- `isSending`: `boolean`, inicial `false`
- `error`: `ProblemDetail | null`, inicial `null`
- Carregar histórico quando `activeSessionId` muda
- Enviar mensagem → `POST /api/chat` → adiciona `userMessage` e `assistantMessage`
- Scroll automático para última mensagem
- Limpar mensagens quando sessão é desselecionada

### Tarefas de implementação
1. Implementar `useMessages`:
   - Estados
   - `loadMessages(sessionId)` → `GET /api/sessions/{id}/messages`
    - `sendMessage(content)` → `POST /api/chat`
    - `useEffect` monitorando `activeSessionId` para carregar histórico
    - Mensagem otimista do usuário substituída pela resposta real
    - Após envio bem-sucedido, chamar callback `onMessageSent` para que `App` invoque `refreshSessions()`
2. Atualizar `App.tsx`:
   - Consumir `useMessages` passando `activeSessionId`
   - Conectar callback `onMessageSent` para chamar `refreshSessions()` do `useSessions`
   - Passar props para `ChatArea`, `ChatInput`
3. Atualizar `ChatArea`:
   - Renderizar lista de `MessageBubble`
   - Placeholder para vazio
   - Skeleton para loading
   - Scroll automático
4. Atualizar `ChatInput`:
   - Input habilitado apenas com sessão ativa
   - Enviar por Enter ou botão
   - Desabilitado durante envio
5. Atualizar `MessageBubble`:
   - Exibir sender, content, timestamp
   - Estilo diferente para USER e ASSISTANT

### Testes manuais recomendados
- Selecionar sessão → histórico carrega
- Enviar mensagem → mensagem do usuário + resposta do assistente aparecem
- Scroll automático funciona
- Sessão sem mensagens → placeholder visível

### Critérios de aceite
- [ ] Histórico carrega ao selecionar sessão
- [ ] Envio de mensagem exibe `userMessage` e `assistantMessage`
- [ ] Scroll automático para última mensagem
- [ ] ChatInput desabilitado sem sessão ativa
- [ ] Estados de loading, vazio e erro funcionais

### Definition of Done
- Commit com `useMessages` e chat funcional

---

## Fase 8: Hook useUpload e Upload Drag-and-Drop

### Objetivo
Implementar o upload de arquivos com drag-and-drop, validação client-side e barra de progresso.

### Dependências
- Fase 3 (uploadService)
- Fase 4 (App, UploadZone, ProgressBar)
- Fase 6 (useSessions → activeSessionId)

### Arquivos e pastas envolvidos
- `src/hooks/useUpload.ts`
- `src/components/UploadZone.tsx` (props atualizadas)
- `src/components/ProgressBar.tsx` (props)
- `src/App.tsx` (consumo do hook)

### Componentes afetados
- `App` (consome `useUpload`, passa props)
- `UploadZone` (recebe `onFileSelected`, `isUploading`, `error`, `isDisabled`)
- `ProgressBar` (recebe `percent`)

### Hooks afetados
- `useUpload`

### Serviços HTTP afetados
- `uploadService`

### Estados e comportamentos
- `isUploading`: `boolean`, inicial `false`
- `progress`: `number`, inicial `0`
- `error`: `ProblemDetail | null`, inicial `null`
- `attachment`: `Attachment | null`, inicial `null`
- Validar tipo (.txt, .pdf), extensão e tamanho (10 MB) no client-side
- Enviar apenas com sessão ativa
- Atualizar progresso durante upload
- Exibir erro com `status`, `title`, `detail` do `ProblemDetail`

### Tarefas de implementação
1. Implementar `useUpload`:
   - Estados
   - `handleFileSelected(file)`:
     - Valida tipo, extensão e tamanho
     - Se inválido → define `error` sem chamar API
     - Se válido e sessão ativa → chama `uploadService.upload` com callback de progresso
    - UploadService retorna erro → exibe `ProblemDetail`
    - Erros client-side (tipo inválido, extensão, tamanho) usam `ProblemDetail` com `status: 400`, `title: "Arquivo inválido"`, `detail` descritivo
    - Após upload bem-sucedido, chamar callback `onUploadComplete` para que `App` invoque `refreshSessions()`
    - Após 3 segundos, feedback é removido
2. Atualizar `UploadZone`:
   - Remover lógica de validação (apenas entrega o arquivo via `onFileSelected`)
   - Drag-and-drop com `onDrop` e `onChange` do input file
   - Desabilitado quando `isDisabled`
   - Exibir `error` quando presente
   - Estados de arrastando (hover visual)
3. Atualizar `ProgressBar`:
   - Visível apenas durante `isUploading`
   - Exibir percentual numérico
   - `role="progressbar"` com atributos ARIA
4. Atualizar `App.tsx`:
   - Consumir `useUpload` passando `activeSessionId`
   - Conectar callback `onUploadComplete` para chamar `refreshSessions()` do `useSessions`
   - Passar props para `UploadZone` e `ProgressBar`

### Testes manuais recomendados
- Arrastar .txt → progresso visível, confirmação ao final
- Arrastar .pdf → mesmo fluxo
- Arrastar tipo inválido → erro imediato sem chamar API
- Arrastar sem sessão ativa → UploadZone desabilitado
- Cancelar upload (se possível) → estado resetado

### Critérios de aceite
- [ ] Upload drag-and-drop funcional para .txt e .pdf
- [ ] Upload apenas com sessão ativa
- [ ] Validação client-side de tipo e tamanho
- [ ] Barra de progresso visível com percentual
- [ ] Erros exibidos com `detail` do `ProblemDetail`
- [ ] UploadZone não contém lógica de negócio

### Definition of Done
- Commit com `useUpload` e upload funcional

---

## Fase 9: Estados de Loading, Vazio e Erro

### Objetivo
Revisar e padronizar todos os estados de loading, sucesso, vazio e erro em cada componente.

### Dependências
- Fases 5, 6, 7, 8 (todos os hooks implementados)

### Arquivos e pastas envolvidos
- Todos os componentes e hooks

### Componentes afetados
- Todos

### Hooks afetados
- Todos

### Serviços HTTP afetados
- Nenhum

### Estados e comportamentos
- Loading: skeletons ou spinners
- Sucesso: conteúdo normal
- Vazio: placeholder amigável
- Erro: mensagem descritiva com `ProblemDetail`

### Tarefas de implementação
1. Revisar `Sidebar`:
   - Loading: skeleton de 3 itens
   - Vazio: "Nenhuma sessão ainda. Crie uma!"
   - Erro: toast/banner com `title` e `detail`
2. Revisar `ChatArea`:
   - Loading: 2–3 skeleton messages
   - Vazio: "Nenhuma mensagem ainda. Escreva algo!"
   - Erro: toast/banner
3. Revisar `UploadZone`:
   - Vazio: "Arraste arquivos ou clique para selecionar"
   - Erro: mensagem inline com `detail`
   - Uploading: input desabilitado
4. Revisar `ChatInput`:
   - Desabilitado se API offline ou sem sessão ativa
5. Revisar `HealthIndicator`:
   - Loading: amarelo "Verificando..."
   - Healthy: verde "API ativa"
   - Unhealthy: vermelho "API indisponível"

### Testes manuais recomendados
- Desligar back-end e verificar estados de erro
- Sessão sem mensagens → placeholder
- Lista de sessões vazia → placeholder
- Upload concluído → toast de sucesso
- Upload com erro → mensagem de erro

### Critérios de aceite
- [ ] Todos os componentes exibem estados de loading corretamente
- [ ] Placeholders visíveis em estados vazios
- [ ] Erros exibem `title` e `detail`
- [ ] Feedback de sucesso no upload

### Definition of Done
- Commit com padronização de estados

---

## Fase 10: Acessibilidade

### Objetivo
Aplicar requisitos básicos de acessibilidade em todos os componentes.

### Dependências
- Fases 4 a 9 (todos os componentes implementados)

### Arquivos e pastas envolvidos
- Todos os componentes

### Componentes afetados
- Todos

### Hooks afetados
- Nenhum

### Serviços HTTP afetados
- Nenhum

### Estados e comportamentos
- Navegação por teclado
- ARIA labels e roles
- Contraste e foco visível

### Tarefas de implementação
1. Adicionar `aria-label` a todos os inputs e botões
2. `MessageBubble`: `role="article"`, `aria-label` informando remetente
3. `ProgressBar`: `role="progressbar"`, `aria-valuenow`, `aria-valuemin`, `aria-valuemax`
4. `UploadZone`: `role="button"`, `tabindex="0"`, ativável por Enter e Espaço
5. Mensagens de erro: `role="alert"`
6. Garantir navegação por Tab entre todos os elementos interativos
7. Verificar contraste mínimo de 4.5:1
8. Garantir foco visível em todos os elementos
9. Adicionar `aria-busy` em estados de carregamento

### Testes manuais recomendados
- Navegar por Tab por toda a interface
- Verificar leitor de tela (NVDA ou VoiceOver)
- Ativar UploadZone por teclado (Enter/Espaço)

### Critérios de aceite
- [ ] Todos os inputs têm `aria-label` ou `<label>`
- [ ] ProgressBar com atributos ARIA
- [ ] UploadZone acessível por teclado
- [ ] Erros têm `role="alert"`
- [ ] Navegação por Tab funcional

### Definition of Done
- Commit com acessibilidade

---

## Fase 11: Integração Final com o Back-end

### Objetivo
Realizar testes ponta a ponta e ajustar detalhes de integração.

### Dependências
- Todas as fases anteriores
- Back-end rodando (Fase 10 do backend-plan)

### Arquivos e pastas envolvidos
- `vite.config.ts` (possível proxy CORS)
- Todos os serviços e hooks

### Componentes afetados
- Todos

### Hooks afetados
- Todos

### Serviços HTTP afetados
- Todos

### Estados e comportamentos
- Fluxo completo usuário

### Tarefas de implementação
1. Configurar proxy no `vite.config.ts` (opcional, como alternativa ao CORS):
   - `server.proxy['/api']` apontando para `http://localhost:8080`
2. Testar fluxo completo:
   - Aplicação inicia → HealthIndicator fica verde
   - Criar sessão → Sidebar atualiza
   - Selecionar sessão → histórico carrega
   - Enviar mensagem → resposta aparece
   - Fazer upload → progresso e confirmação
   - Upload sem sessão → desabilitado
   - Upload tipo inválido → erro imediato
3. Validar estados de erro com back-end desligado
4. Validar formatação de datas ISO 8601 UTC
5. Ajustar CSS fino para alinhamento e responsividade

### Testes manuais recomendados
- Fluxo completo de A a Z
- Desconectar back-end e verificar comportamento degradado
- Testar em janela reduzida (responsivo mínimo)

### Critérios de aceite
- Todos os itens da seção "Critérios de aceite da primeira etapa" da especificação

### Definition of Done
- Aplicação funcional ponta a ponta
- Checklist final assinado

---

## Seção Final

### Dependências entre fases
```
Fase 1 (bootstrap)
  └── Fase 2 (tipos)
        └── Fase 3 (serviços HTTP)
              ├── Fase 4 (layout base)
              │     ├── Fase 5 (useHealth + HealthIndicator)
              │     ├── Fase 6 (useSessions + Sidebar)
              │     │     ├── Fase 7 (useMessages + Chat)
              │     │     └── Fase 8 (useUpload + UploadZone)
              │     └── ...
              └── Fase 9 (estados) — depende de 5, 6, 7, 8
Fase 10 (acessibilidade) — depende de 4 a 9
Fase 11 (integração final) — depende de todas
```

### Tarefas em paralelo
- **Fase 5 (health)**, **Fase 6 (sessions)** e **Fase 8 (upload)** podem ser implementadas em paralelo
- **Fase 9 (estados)** e **Fase 10 (acessibilidade)** podem ser feitas em paralelo
- **Fase 7 (chat)** depende da Fase 6 (activeSessionId)

### Tarefas que dependem do outro repositório
- **Fase 3 (serviços HTTP):** contratos definidos, independente
- **Fase 5 a 8 (hooks):** dependem do back-end rodando para testes reais, mas podem ser implementadas com dados mock
- **Fase 11 (integração final):** requer back-end funcional

### Riscos de integração
- CORS: Vite na porta 5173, back-end na 8080 — necessário configurar proxy ou CORS
- Formato de datas: front-end deve interpretar ISO 8601 UTC corretamente (usar `Intl.DateTimeFormat` ou biblioteca leve)
- `ProblemDetail`: garantir que o front-end trate corretamente os campos `status`, `title`, `detail`
- Upload com `onUploadProgress` do axios depende do servidor enviar header `Content-Length`
- Se o back-end não estiver disponível durante o desenvolvimento, usar dados mock nos hooks

### Checklist final da primeira etapa
- [ ] HealthIndicator exibe estado da API (verde/amarelo/vermelho)
- [ ] Sidebar carrega e exibe lista de sessões
- [ ] Botão "[+ Nova]" cria sessão e a seleciona automaticamente
- [ ] Clique em sessão carrega histórico de mensagens
- [ ] Input de texto envia mensagem e exibe `userMessage` e `assistantMessage`
- [ ] ChatArea faz scroll automático para última mensagem
- [ ] Upload drag-and-drop aceita .txt e .pdf e exibe progresso
- [ ] Upload permitido apenas com sessão ativa
- [ ] Upload de tipo inválido exibe erro imediato (sem chamar API)
- [ ] Upload com sessionId inválido exibe erro com `detail` do `ProblemDetail`
- [ ] Estados de loading exibidos com skeleton ou spinner
- [ ] Estados de vazio exibem placeholder amigável
- [ ] Estados de erro exibem `title` e `detail` do `ProblemDetail`
- [ ] Todos os elementos interativos acessíveis por teclado
- [ ] Nenhum componente chama API diretamente
- [ ] Nenhum hook contém JSX
- [ ] `App` não contém estado próprio — apenas consome hooks

### Sugestão de branches por funcionalidade
| Branch | Conteúdo |
|--------|----------|
| `main` | versão estável |
| `feature/bootstrap` | Fase 1 |
| `feature/types` | Fase 2 |
| `feature/services` | Fase 3 |
| `feature/layout` | Fase 4 |
| `feature/use-health` | Fase 5 |
| `feature/use-sessions` | Fase 6 |
| `feature/use-messages` | Fase 7 |
| `feature/use-upload` | Fase 8 |
| `feature/states` | Fase 9 |
| `feature/accessibility` | Fase 10 |
| `feature/integration` | Fase 11 |
