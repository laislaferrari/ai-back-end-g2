# Configuração do Ollama

## Para que o Ollama é usado no projeto

O Ollama fornece dois serviços essenciais para o pipeline RAG:

- **Embeddings**: o `OllamaEmbeddingService` utiliza o modelo `embeddinggemma:300m` para gerar vetores de 768 dimensões a partir do texto dos documentos e das perguntas do usuário. Esses vetores são armazenados no PostgreSQL com pgvector e usados para busca semântica.
- **Geração de resposta**: o `OllamaAiResponseGenerator` utiliza o modelo `llama3.2:3b` para gerar respostas contextuais durante o chat. O conteúdo dos documentos indexados é injetado como contexto para fundamentar a resposta do assistente.

Ambos os serviços são ativados apenas no perfil `postgres`:

- `OllamaEmbeddingService`: ativo em todos os perfis exceto `test` (`@Profile("!test")`)
- `OllamaAiResponseGenerator`: ativo apenas no perfil `postgres` (`@Profile("postgres & !test")`)

No perfil `default` (H2), o projeto utiliza implementações mock que não dependem do Ollama.

## Modelos utilizados

| Modelo | Finalidade | Dimensão |
|---|---|---|
| `embeddinggemma:300m` | Embeddings (geração de vetores) | 768 |
| `llama3.2:3b` | Geração de texto (chat contextual) | — |

## Instalação

### Windows

Baixe o instalador no site oficial:

[https://ollama.com](https://ollama.com)

Execute o instalador e siga as instruções. O Ollama será registrado como serviço do Windows e iniciado automaticamente.

### macOS

```bash
brew install ollama
```

### Linux

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

## Como iniciar o serviço

### Windows

O Ollama é iniciado automaticamente como serviço do Windows após a instalação. Para verificar, procure por "Ollama" no Gerenciador de Serviços ou execute:

```powershell
ollama --version
```

Se o servidor ainda não estiver ativo, abra o aplicativo Ollama ou execute em um terminal:

```powershell
ollama serve
```

### macOS / Linux

```bash
ollama serve
```

Mantenha o terminal aberto enquanto o backend estiver em execução.

## Verificar se o serviço está rodando

```bash
ollama --version
```

Se o comando retornar a versão, o Ollama está instalado. Para confirmar se o servidor está aceitando requisições:

#### Windows PowerShell

```powershell
Invoke-RestMethod -Uri "http://localhost:11434/api/tags" -Method Get
```

#### macOS / Linux / Git Bash

```bash
curl http://localhost:11434/api/tags
```

A resposta esperada é um JSON com a lista de modelos disponíveis.

## Baixar os modelos

```bash
ollama pull embeddinggemma:300m
ollama pull llama3.2:3b
```

O download pode levar alguns minutos dependendo da conexão. Os modelos são armazenados em cache local e não precisam ser baixados novamente.

## Listar modelos baixados

```bash
ollama list
```

## Variáveis de ambiente OLLAMA_*

O projeto expõe as seguintes variáveis para configurar a conexão com o Ollama:

| Variável | Obrigatória | Padrão | Descrição |
|---|---|---|---|
| `OLLAMA_URL` | não | `http://localhost:11434/api/embed` | URL do endpoint de embeddings |
| `OLLAMA_MODEL` | não | `embeddinggemma:300m` | Modelo usado para gerar embeddings |
| `OLLAMA_CHAT_URL` | não | `http://localhost:11434/api/chat` | URL do endpoint de chat |
| `OLLAMA_CHAT_MODEL` | não | `llama3.2:3b` | Modelo usado para gerar respostas |
| `OLLAMA_CHAT_TEMPERATURE` | não | `0.2` | Temperatura do modelo de geração (0.0 a 2.0) |

Todas as variáveis possuem valores padrão e são opcionais. Configure apenas se precisar alterar a URL padrão ou usar modelos diferentes.

As validações aplicadas pela aplicação:

- `rag.generation.ollama-url` (`OLLAMA_CHAT_URL`) não pode ser vazio.
- `rag.generation.model` (`OLLAMA_CHAT_MODEL`) não pode ser vazio.
- `rag.generation.temperature` (`OLLAMA_CHAT_TEMPERATURE`) deve estar entre 0.0 e 2.0.

## Como testar se o Ollama está acessível

### Endpoint de embeddings

#### Windows PowerShell

```powershell
$body = @{
    model = "embeddinggemma:300m"
    input = "teste"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:11434/api/embed" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

#### macOS / Linux / Git Bash

```bash
curl -X POST http://localhost:11434/api/embed \
  -H "Content-Type: application/json" \
  -d '{"model": "embeddinggemma:300m", "input": "teste"}'
```

A resposta deve conter o campo `embeddings` com um array de números.

### Endpoint de chat

#### Windows PowerShell

```powershell
$body = @{
    model = "llama3.2:3b"
    messages = @(
        @{
            role = "user"
            content = "Olá"
        }
    )
    stream = $false
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:11434/api/chat" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

#### macOS / Linux / Git Bash

```bash
curl -X POST http://localhost:11434/api/chat \
  -H "Content-Type: application/json" \
  -d '{"model": "llama3.2:3b", "messages": [{"role": "user", "content": "Olá"}], "stream": false}'
```

A resposta deve conter o campo `message` com `content`.

## Problemas comuns

### Connection refused

O backend não consegue se conectar ao Ollama.

1. Confirme que o Ollama está rodando: `ollama --version`
2. Verifique se o servidor está ativo: `curl http://localhost:11434/api/tags`
3. Confirme que a URL configurada no `.env` corresponde à URL real do Ollama.
4. No Windows, abra o aplicativo Ollama ou execute `ollama serve` em um terminal.

### Modelo ausente

Erro `"model not found"` ao chamar o Ollama.

Execute os comandos de download:

```bash
ollama pull embeddinggemma:300m
ollama pull llama3.2:3b
```

Verifique se os modelos aparecem na lista:

```bash
ollama list
```

### URL incorreta

A aplicação valida que `OLLAMA_CHAT_URL` não pode estar vazio. Se a URL estiver incorreta, o backend lançará `GenerationException` com a mensagem "Falha de conexão com o serviço de geração."

Verifique os valores no `.env`:

```env
OLLAMA_URL=http://localhost:11434/api/embed
OLLAMA_CHAT_URL=http://localhost:11434/api/chat
```

A porta padrão do Ollama é `11434`. Se o Ollama estiver rodando em outra porta, ajuste as variáveis.

### Timeout

O projeto não configura timeout explicitamente no `RestTemplate` utilizado para se comunicar com o Ollama.

Soluções:

1. Reduza o tamanho dos documentos enviados.
2. Verifique se há outros processos consumindo GPU/CPU.
3. Se necessário, implemente um `RestTemplate` customizado com timeout definido via código.

### Dimensão de embedding incompatível

Erro na ingestão com a mensagem:

```
Dimensão do embedding retornada (N) difere da esperada (768).
```

O projeto espera vetores de **768 dimensões**, que é a saída do `embeddinggemma:300m`. Esse erro ocorre quando:

1. Um modelo diferente é configurado em `OLLAMA_MODEL` e ele gera embeddings com outra dimensão.
2. O Ollama retorna um embedding com formato inesperado.

Soluções:

1. Use `embeddinggemma:300m` como modelo de embedding.
2. Se precisar usar outro modelo, altere `RAG_EMBEDDING_DIMENSION` no `application-postgres.yml` para a dimensão correta do modelo.
3. Teste o endpoint de embedding manualmente para conferir a dimensão retornada.
