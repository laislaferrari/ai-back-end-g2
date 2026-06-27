# Checklist de entrega — Parte 2

## Backend
- [ ] PostgreSQL configurado
- [ ] pgvector habilitado
- [ ] migrations reproduzíveis
- [ ] documentos persistidos
- [ ] chunks persistidos com embedding
- [ ] status do documento
- [ ] parsing separado
- [ ] chunking separado
- [ ] embedding separado
- [ ] RagService testável
- [ ] topK configurável
- [ ] minSimilarity configurável
- [ ] chat retorna sources
- [ ] erros 400/404/409/500
- [ ] webhook n8n
- [ ] testes passando

## Frontend
- [ ] tipos atualizados
- [ ] estado captura sources
- [ ] MessageBubble exibe fontes
- [ ] SourcePanel criado
- [ ] loading
- [ ] resposta sem fontes
- [ ] erro
- [ ] build passando

## n8n
- [ ] workflow importado
- [ ] health validado
- [ ] status consultado
- [ ] notificação entregue
- [ ] execução registrada

## Demonstração
- [ ] subir banco
- [ ] subir backend
- [ ] subir frontend
- [ ] subir n8n
- [ ] enviar documento conhecido
- [ ] aguardar INDEXED
- [ ] perguntar algo presente no documento
- [ ] mostrar fontes corretas
- [ ] perguntar algo fora do documento
- [ ] mostrar resposta sem fontes
