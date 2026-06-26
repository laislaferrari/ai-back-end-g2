Comunicação entre Front-end e Back-end

Base URL:
http://localhost:8080/api

Endpoints previstos:

GET /health
Verifica se a API está ativa.

POST /chat
Envia uma mensagem do usuário e recebe uma resposta textual.

GET /sessions
Lista as sessões de conversa.

GET /sessions/{id}/messages
Busca o histórico de mensagens de uma sessão.

POST /upload
Envia arquivos TXT ou PDF usando multipart/form-data.

Formato de mensagem:
{
  "sessionId": 1,
  "content": "Hoje foi um dia difícil..."
}

Formato de resposta:
{
  "sessionId": 1,
  "sender": "assistant",
  "content": "Entendi. Você deseja registrar esse sentimento no diário?"
}