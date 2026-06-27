# Sugestão de branches — Parte 2

## Backend
- `feature/postgres-pgvector`
- `feature/document-domain`
- `feature/document-parsing`
- `feature/document-chunking`
- `feature/embedding-service`
- `feature/document-ingestion`
- `feature/rag-retrieval`
- `feature/chat-sources`
- `feature/document-status`
- `feature/n8n-webhook`
- `feature/rag-tests`
- `feature/docs-part-2`

## Frontend
- `feature/rag-contracts`
- `feature/chat-sources-state`
- `feature/source-panel`
- `feature/document-status-ui`
- `feature/rag-error-states`

## Integração
Cada branch deve:
1. nascer da `main` atualizada;
2. alterar apenas uma responsabilidade;
3. incluir teste ou roteiro de validação;
4. entrar via Pull Request;
5. ser integrada antes da branch dependente.
