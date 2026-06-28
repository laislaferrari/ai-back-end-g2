# Decisão Arquitetural: Provedor de Embeddings para o Pipeline RAG

## Status

Decisão aprovada. Implementação ainda futura.

## Contexto

- Projeto acadêmico sem orçamento para API paga.
- Necessidade de gerar embeddings para o pipeline RAG (Retrieval-Augmented Generation).
- A dimensão do vetor impacta diretamente a DDL da migration V3 (coluna `vector(D)`).
- Necessário definir provedor, modelo e dimensão antes da migration V3.

## Alternativas consideradas

### API externa

| Aspecto | Avaliação |
|---|---|
| Custo | Inviável -- sem orçamento acadêmico |
| Dependência de internet | Obrigatória |
| Necessidade de chave | Sim |
| Privacidade | Dados enviados a terceiros |
| Facilidade de reprodução | Dependente de conta e saldo |
| Consumo local de recursos | Nenhum |

**Descartada por custo e dependência externa.**

### Ollama em Docker

| Aspecto | Avaliação |
|---|---|
| Custo | Zero |
| Container adicional | Sim -- mais um serviço no Docker Compose |
| Rede e volume | Mapeamento de porta e montagem de volume adicionais |
| Complexidade de setup | Maior -- Docker se tornaria obrigatório também para quem trabalha apenas com H2 |

**Descartada como obrigatória.** Adiciona complexidade desnecessária a um serviço que pode ser executado nativamente no sistema operacional.

### Ollama nativo (escolhido)

| Aspecto | Avaliação |
|---|---|
| Custo | Zero |
| Instalação | Específica por sistema operacional |
| Dependência de internet em runtime | Não, após o modelo estar instalado |
| Consumo de recursos | Utiliza recursos da máquina local |
| Docker | Não é necessário para executar o Ollama |

## Decisão

- **Provedor:** Ollama local
- **Modelo:** `embeddinggemma:300m`
- **URL base padrão:** `http://localhost:11434`
- **Endpoint derivado:** `{baseUrl}/api/embed`
- **Dimensão validada:** `768`
- **Futura coluna PostgreSQL:** `vector(768)`
- **Instalação:** nativa no sistema operacional
- **Ollama fora do Docker Compose principal**

## Métrica vetorial

```
Distância cosseno no pgvector:
  embedding <=> query_embedding

Similaridade:
  1 - (embedding <=> query_embedding)
```

## Contrato conceitual da API

### Requisição

```json
{
  "model": "embeddinggemma:300m",
  "input": "Texto do fragmento para gerar embedding"
}
```

### Resposta (shape relevante)

```json
{
  "embeddings": [
    [0.0123, -0.0456, 0.0789]
  ]
}
```

**Observações:**

- O campo correto da resposta é `embeddings`, no plural.
- Ele contém uma lista de vetores (um vetor por entrada textual).
- Para uma única entrada textual, utilizar conceitualmente `embeddings[0]`.
- `embeddings[0]` deve conter exatamente 768 valores numéricos.

## Motivo para não incluir no Docker Compose

- Ollama é uma dependência externa de runtime para geração de embeddings e indexação.
- A indisponibilidade do Ollama não deve impedir funcionalidades da Parte 1 nem testes com H2.
- Chamadas de indexação devem falhar de forma controlada quando Ollama estiver indisponível.
- PostgreSQL com pgvector permanece no Docker Compose principal.
- Ollama permanece nativo e externo ao Compose.

## Compatibilidade entre sistemas operacionais

| Sistema | Procedimento | Situação |
|---|---|---|
| macOS Apple Silicon | Homebrew | Validado localmente |
| Windows | Instalador oficial ou winget | Previsto, pendente de confirmação pela equipe |
| Linux | Script oficial | Previsto, pendente de confirmação pela equipe |

## Configuração futura

```yaml
rag:
  embedding:
    base-url: ${RAG_EMBEDDING_BASE_URL:http://localhost:11434}
    model: ${RAG_EMBEDDING_MODEL:embeddinggemma:300m}
    dimension: ${RAG_EMBEDDING_DIMENSION:768}
```

O endpoint completo será derivado como:

```
{baseUrl}/api/embed
```

Não criar variável separada para endpoint completo.

## Estratégia de validação

- **Startup (local):** validar que `rag.embedding.base-url`, `rag.embedding.model` e `rag.embedding.dimension` estão preenchidos; validar que a dimensão configurada é positiva e igual a 768.
- **Startup (remoto):** não realizar obrigatoriamente chamada remota que impeça o startup. Validação remota estrita poderá ser futura e configurável.
- **Health/readiness:** conectividade com o endpoint `/api/embed` poderá ser exposta como indicador de prontidão.
- **Runtime:** toda resposta da API deve validar que `embeddings[0].length == 768`. Ausência, lista vazia, formato inválido ou dimensão incorreta deve gerar futura `EmbeddingException`.
- **Testes H2 e Parte 1:** não devem exigir Ollama.

## Impacto futuro

### Migration V3

A coluna `embedding` da tabela `document_chunks` será definida como `vector(768)`.

### EmbeddingService

A interface `EmbeddingService` (pacote `com.mindjournal.service.embedding`) permanece conforme especificada em `docs/parte-2/rag-domain-spec.md`. A implementação concreta `OllamaEmbeddingService` (futura) realizará:

1. HTTP POST para `{baseUrl}/api/embed` com `{"model": "{model}", "input": "{text}"}`.
2. Acessar conceitualmente `embeddings[0]`.
3. Validar que o vetor contém exatamente a dimensão configurada.
4. Converter a resposta para a representação Java que será definida após a validação técnica de Hibernate, PostgreSQL e pgvector.

### Tratamento de erros

A implementação deve tratar:

- Timeout de conexão.
- Serviço indisponível (HTTP 5xx ou conexão recusada).
- Erro HTTP (4xx).
- Resposta sem campo `embeddings`.
- Lista `embeddings` vazia.
- Dimensão de `embeddings[0]` diferente do esperado.
- Erro de desserialização.

Todos os casos devem lançar `EmbeddingException`.

## Riscos e limitações

| Risco | Impacto | Mitigação |
|---|---|---|
| Ollama não instalado | Indexação falha | Documentação clara; erro amigável em tempo de indexação |
| Serviço Ollama não iniciado | Chamadas de embedding falham | Erro tratado como `EmbeddingException` |
| Modelo não baixado | `/api/embed` retorna erro | `ollama pull embeddinggemma:300m` incluso no setup |
| Dimensão incompatível | Inconsistência com coluna `vector(768)` | Validação em runtime de toda resposta |
| Resposta sem campo `embeddings` | Desserialização falha | Tratamento com `EmbeddingException` |
| Lista `embeddings` vazia | Nenhum vetor disponível | Tratamento com `EmbeddingException` |
| Consumo de CPU/memória pelo Ollama | Impacto no desempenho local | Validar o consumo nos ambientes da equipe e evitar processamento concorrente sem medição prévia |
| Porta 11434 ocupada | EmbeddingService falha | Variável `RAG_EMBEDDING_BASE_URL` permite reconfigurar |
| Procedimentos Windows/Linux pendentes | Setup incompleto para alguns integrantes | Procedimento previsto; aguardando confirmação |

## Reprodução local

```bash
# macOS
brew install ollama

# Windows
winget install Ollama.Ollama

# Linux
curl -fsSL https://ollama.com/install.sh | sh

ollama serve
ollama pull embeddinggemma:300m
```

Para iniciar o backend com PostgreSQL:

```bash
bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Decisões pendentes

Permanece pendente e **bloqueando a migration V3**:

- Representação Java do vetor.
- Integração Hibernate 6, driver PostgreSQL e pgvector.
- Uso de `float[]` com `columnDefinition`, biblioteca específica ou tipo customizado.
- Validação técnica antes da migration V3.
