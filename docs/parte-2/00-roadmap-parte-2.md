# MindJourney IA — Roadmap da Parte 2

## Objetivo
Evoluir a aplicação atual para um fluxo RAG completo:

1. upload e registro do documento;
2. parsing do conteúdo;
3. fragmentação em chunks;
4. geração de embeddings;
5. persistência no PostgreSQL com pgvector;
6. recuperação por similaridade;
7. resposta do chat com fontes;
8. notificação assíncrona por n8n;
9. exibição das fontes no frontend.

## Ordem segura de implementação

### Etapa 1 — Contratos
- Aprovar os System Docs.
- Definir DTOs de documento, status, fonte e resposta do chat.
- Definir estados: `RECEIVED`, `PROCESSING`, `INDEXED`, `FAILED`.
- Definir o payload do webhook do n8n.

### Etapa 2 — Infraestrutura
- Migrar o ambiente da Parte 2 para PostgreSQL.
- Habilitar a extensão pgvector.
- Adicionar migrations.
- Manter segredos em variáveis de ambiente.

### Etapa 3 — Ingestão
- Implementar `DocumentParser`.
- Implementar `TextChunker`.
- Implementar `EmbeddingService`.
- Implementar `DocumentIngestionService`.
- Persistir documento e chunks.
- Atualizar status.
- Disparar webhook do n8n.

### Etapa 4 — Retrieval
- Implementar busca vetorial.
- Implementar `RagService`.
- Configurar `topK` e `minSimilarity`.
- Retornar fontes no contrato do chat.

### Etapa 5 — Frontend
- Atualizar tipos TypeScript.
- Capturar `sources` no estado da conversa.
- Exibir fontes recolhíveis em `MessageBubble`.
- Criar `SourcePanel`.

### Etapa 6 — n8n
- Criar workflow de documento indexado.
- Consultar `/api/documents/{id}/status`.
- Consultar `/api/health`.
- Entregar notificação.

### Etapa 7 — Validação
- Testar contratos isoladamente.
- Testar ingestão com documento conhecido.
- Testar resposta com e sem fontes.
- Testar estados de loading e erro.
- Testar webhook e execução do n8n.
