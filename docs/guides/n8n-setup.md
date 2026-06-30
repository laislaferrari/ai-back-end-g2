# Configuração do n8n

## Para que o n8n é usado no projeto

O n8n recebe uma notificação sempre que um documento enviado pelo usuário termina de ser indexado no pipeline RAG. O workflow importado consulta o health check do backend e o status do documento, permitindo acompanhar remotamente o resultado da indexação.

O n8n é **opcional** para o funcionamento do chat e do pipeline RAG. Quando `N8N_DOCUMENT_INDEXED_URL` não é configurada, um log informativo é gerado e a indexação prossegue normalmente.

## Como a notificação funciona

Após a indexação de um documento (status `INDEXED`), a classe `N8nWebhookDocumentIndexingNotifier` envia uma requisição HTTP POST para a URL configurada em `N8N_DOCUMENT_INDEXED_URL`.

O payload enviado contém:

```json
{
  "event": "DOCUMENT_INDEXED",
  "documentId": 1,
  "fileName": "exemplo.txt",
  "status": "INDEXED",
  "indexedChunks": 5,
  "timestamp": "2026-06-30T10:00:00Z"
}
```

Falhas na notificação são registradas como aviso nos logs do backend (`log.warn`) e não interrompem o pipeline.

## Executar o n8n localmente

### Windows PowerShell

```powershell
docker run -it --rm --name n8n -p 5678:5678 n8nio/n8n
```

### macOS / Linux / Git Bash

```bash
docker run -it --rm --name n8n -p 5678:5678 n8nio/n8n
```

O n8n também pode ser executado sem Docker:

```bash
npx n8n
```

## Acessar a interface

Após iniciar o n8n, acesse:

```
http://localhost:5678
```

No primeiro acesso, o n8n solicitará a criação de um usuário e senha locais.

## Importar o workflow

1. Na interface do n8n, clique em **Workflows** > **Add Workflow**.
2. Clique em **Import from File** (ícone de upload na barra superior).
3. Selecione o arquivo:

```
n8n/workflows/document-indexed-notification.json
```

O workflow importado contém os seguintes nós:

| Nó | Função |
|---|---|
| Webhook - Receber Notificação | Recebe POST em `/webhook/document-indexed` |
| Edit Fields | Transformação dos dados recebidos |
| GET - Health Check | Consulta `GET /api/health` do backend |
| GET - Status do Documento | Consulta `GET /api/documents/{id}/status` |
| IF - Status Indexado? | Verifica se o status é `INDEXED` |
| Notificação de Sucesso | Ramo quando o status é INDEXED |
| Notificação de Falha | Ramo quando o status não é INDEXED |
| Resposta do Webhook | Retorna a resposta ao chamador |

## Revisar e publicar o workflow

1. Abra o workflow importado.
2. Revise as configurações dos nós HTTP Request, em especial a URL base `host.docker.internal:8080`.
3. Clique em **Save** (Ctrl+S / Cmd+S).
4. Clique em **Active** (toggle no canto superior direito) para publicar e ativar o workflow.

O workflow só começa a receber requisições quando estiver ativo.

## Método e caminho do webhook

O workflow escuta requisições **POST** no caminho:

```
/webhook/document-indexed
```

A URL completa do webhook gerada pelo n8n segue o padrão:

```
http://localhost:5678/webhook/document-indexed
```

Essa URL deve ser copiada e configurada no backend.

## Configurar a variável de ambiente

Edite o arquivo `.env` na raiz do projeto e defina:

```env
N8N_DOCUMENT_INDEXED_URL=http://localhost:5678/webhook/document-indexed
```

Se o n8n estiver rodando em Docker, use `host.docker.internal` no lugar de `localhost`:

```env
N8N_DOCUMENT_INDEXED_URL=http://host.docker.internal:5678/webhook/document-indexed
```

Para desativar a notificação, deixe o valor vazio ou remova a linha.

## Como o backend envia o documentId

A classe `N8nWebhookDocumentIndexingNotifier` envia o `documentId` (e demais campos) no corpo da requisição POST para a URL configurada. O workflow do n8n extrai o `documentId` com a expressão:

```
{{ $json.body.documentId }}
```

e o utiliza para montar a URL de consulta de status:

```
http://host.docker.internal:8080/api/documents/{{ $json.body.documentId }}/status
```

## Como o workflow consulta o backend

O workflow executa duas requisições HTTP em sequência:

1. **Health check** — GET `http://host.docker.internal:8080/api/health` — confirma que o backend está acessível.
2. **Status do documento** — GET `http://host.docker.internal:8080/api/documents/{documentId}/status` — obtém o status atual do documento.

Ambas as requisições têm timeout de 10 segundos configurado no nó HTTP Request.

Se o status retornado for `INDEXED`, o workflow segue para o ramo de sucesso. Caso contrário, segue para o ramo de falha.

## Uso de host.docker.internal

O workflow utiliza `host.docker.internal` como host para acessar o backend. Esse DNS é resolvido pelo Docker no Windows e macOS para o endereço da máquina host.

Se o n8n estiver rodando localmente (sem Docker) e o backend também estiver local, substitua `host.docker.internal` por `localhost` nos nós de HTTP Request.

## Testar o webhook manualmente

Com o n8n rodando e o workflow ativo, envie uma requisição POST simulando a notificação.

### Windows PowerShell

```powershell
$body = @{
    event = "DOCUMENT_INDEXED"
    documentId = 1
    fileName = "teste.txt"
    status = "INDEXED"
    indexedChunks = 3
    timestamp = (Get-Date).ToString("o")
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:5678/webhook/document-indexed" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

### macOS / Linux / Git Bash

```bash
curl -X POST http://localhost:5678/webhook/document-indexed \
  -H "Content-Type: application/json" \
  -d '{
    "event": "DOCUMENT_INDEXED",
    "documentId": 1,
    "fileName": "teste.txt",
    "status": "INDEXED",
    "indexedChunks": 3,
    "timestamp": "2026-06-30T10:00:00Z"
  }'
```

Para testar com um documento real, faça upload de um arquivo via `POST /api/upload` e acompanhe os logs do backend — a notificação será enviada automaticamente após a indexação.

## Verificar execução com status de sucesso

1. Na interface do n8n, acesse **Executions** no menu lateral.
2. Localize a execução mais recente do workflow `Document Indexed Notification`.
3. Clique na execução para ver o detalhamento nó a nó.
4. Verifique se todos os nós estão verdes (sucesso) e se o nó "IF - Status Indexado?" seguiu para o ramo "Notificação de Sucesso".

Uma execução bem-sucedida mostra a sequência completa:

```
Webhook → Edit Fields → Health Check → Status do Documento → IF (INDEXED) → Sucesso → Resposta do Webhook
```

## Problemas comuns

### Webhook não encontrado

O n8n retorna 404 ao chamar o webhook.

1. Confirme que o workflow está com o toggle **Active** ligado.
2. Verifique se o caminho do webhook está correto: `/webhook/document-indexed`.
3. Certifique-se de que o n8n está rodando na porta `5678`.

### Método HTTP incorreto

O workflow espera requisições **POST**. Se enviar GET ou outro método, o n8n retornará erro.

Verifique se o método configurado na chamada é `POST`.

### Workflow não publicado

Um workflow salvo mas não ativo não recebe requisições. Ative o toggle **Active** no editor do workflow.

### Backend inacessível pelo container

Os nós de HTTP Request usam `host.docker.internal:8080`. Se o n8n estiver em Docker e o backend não estiver acessível:

1. Teste o health check manualmente dentro do container: `docker exec -it n8n curl http://host.docker.internal:8080/api/health`
2. No Linux, `host.docker.internal` não é resolvido por padrão — use `--add-host host.docker.internal:host-gateway` ao iniciar o container n8n:

```bash
docker run -it --rm --name n8n -p 5678:5678 --add-host host.docker.internal:host-gateway n8nio/n8n
```

3. Se o n8n estiver rodando localmente (sem Docker), substitua `host.docker.internal` por `localhost` nos URLs dos nós HTTP Request.

### documentId ausente

Se o payload enviado não contiver `documentId`, o nó "GET - Status do Documento" tentará acessar uma URL como `/api/documents/null/status`.

Verifique o payload recebido no nó "Webhook - Receber Notificação" através do painel de execuções do n8n.

### Documento ainda não INDEXED

O nó "IF - Status Indexado?" compara o campo `status` com o valor `INDEXED`. Se o documento ainda estiver em `RECEIVED` ou `PROCESSING`, o workflow segue para "Notificação de Falha".

Isso é esperado se a notificação for enviada antes da indexação terminar. Verifique o status real do documento via `GET /api/documents/{id}/status`.
