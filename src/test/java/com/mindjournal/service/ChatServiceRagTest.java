package com.mindjournal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mindjournal.dto.ChatRequest;
import com.mindjournal.dto.ChatResponse;
import com.mindjournal.dto.MessageResponse;
import com.mindjournal.dto.SourceDTO;
import com.mindjournal.entity.MessageRole;
import com.mindjournal.entity.Session;
import com.mindjournal.exception.EmbeddingException;
import com.mindjournal.exception.SessionNotFoundException;
import com.mindjournal.repository.MessageRepository;
import com.mindjournal.repository.SessionRepository;
import com.mindjournal.service.rag.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import com.mindjournal.service.rag.RagContext;
import com.mindjournal.service.rag.RagService;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ChatServiceRagTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private AiResponseGenerator aiResponseGenerator;

    @Mock
    private RagService ragService;

    @Mock
    private ObjectProvider<RagService> ragServiceProvider;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private TitleGeneratorService titleGeneratorService;

    private ChatService chatService;

    private Session session;
    private ChatRequest request;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                sessionRepository, messageService, aiResponseGenerator, ragServiceProvider,
                messageRepository, titleGeneratorService
        );
        when(titleGeneratorService.generateTitle(anyString())).thenReturn("Título gerado");

        session = new Session();
        session.setId(10L);
        session.setTitle("Teste");
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());

        request = new ChatRequest(10L, "Minha pergunta");
    }

    @Test
    @DisplayName("sendMessage retorna ChatResponse com sources quando RagService está disponível e encontra chunks")
    void sendMessageWithRagServiceAndSources() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        MessageResponse userMsg = new MessageResponse(1L, "Minha pergunta", MessageRole.USER, Instant.now());
        when(messageService.createAndSaveMessage(session, MessageRole.USER, "Minha pergunta"))
                .thenReturn(userMsg);

        when(ragServiceProvider.getIfAvailable()).thenReturn(ragService);

        List<SourceDTO> sources = List.of(
                new SourceDTO(5L, "doc.pdf", 22L, "trecho relevante", 0.92)
        );
        RagContext ragContext = new RagContext("trecho relevante", sources);
        when(ragService.retrieveContext(10L, "Minha pergunta")).thenReturn(ragContext);

        when(aiResponseGenerator.generateResponse(eq(10L), eq("Minha pergunta"), anyString()))
                .thenReturn("Resposta da IA");

        MessageResponse assistantMsg = new MessageResponse(2L, "Resposta da IA", MessageRole.ASSISTANT, Instant.now());
        when(messageService.createAndSaveMessage(session, MessageRole.ASSISTANT, "Resposta da IA"))
                .thenReturn(assistantMsg);

        ChatResponse response = chatService.sendMessage(request);

        assertEquals(userMsg, response.userMessage());
        assertEquals(assistantMsg, response.assistantMessage());
        assertEquals(1, response.sources().size());
        assertEquals("doc.pdf", response.sources().get(0).fileName());
        assertEquals(0.92, response.sources().get(0).similarityScore(), 0.001);
    }

    @Test
    @DisplayName("sendMessage retorna sources vazio quando RagService não encontra chunks relevantes")
    void sendMessageWithRagServiceAndEmptySources() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        MessageResponse userMsg = new MessageResponse(1L, "Minha pergunta", MessageRole.USER, Instant.now());
        when(messageService.createAndSaveMessage(session, MessageRole.USER, "Minha pergunta"))
                .thenReturn(userMsg);

        when(ragServiceProvider.getIfAvailable()).thenReturn(ragService);

        RagContext emptyContext = new RagContext("", Collections.emptyList());
        when(ragService.retrieveContext(10L, "Minha pergunta")).thenReturn(emptyContext);

        when(aiResponseGenerator.generateResponse(eq(10L), eq("Minha pergunta"), anyString()))
                .thenReturn("Resposta sem contexto");

        MessageResponse assistantMsg = new MessageResponse(2L, "Resposta sem contexto", MessageRole.ASSISTANT, Instant.now());
        when(messageService.createAndSaveMessage(session, MessageRole.ASSISTANT, "Resposta sem contexto"))
                .thenReturn(assistantMsg);

        ChatResponse response = chatService.sendMessage(request);

        assertTrue(response.sources().isEmpty());
    }

    @Test
    @DisplayName("sendMessage retorna sources vazio quando RagService não está disponível")
    void sendMessageWithoutRagService() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        MessageResponse userMsg = new MessageResponse(1L, "Minha pergunta", MessageRole.USER, Instant.now());
        when(messageService.createAndSaveMessage(session, MessageRole.USER, "Minha pergunta"))
                .thenReturn(userMsg);

        when(ragServiceProvider.getIfAvailable()).thenReturn(null);

        when(aiResponseGenerator.generateResponse(eq(10L), eq("Minha pergunta"), anyString()))
                .thenReturn("Resposta sem RAG");

        MessageResponse assistantMsg = new MessageResponse(2L, "Resposta sem RAG", MessageRole.ASSISTANT, Instant.now());
        when(messageService.createAndSaveMessage(session, MessageRole.ASSISTANT, "Resposta sem RAG"))
                .thenReturn(assistantMsg);

        ChatResponse response = chatService.sendMessage(request);

        assertTrue(response.sources().isEmpty());
        assertEquals("Resposta sem RAG", response.assistantMessage().content());
    }

    @Test
    @DisplayName("sendMessage lança SessionNotFoundException quando sessão não existe")
    void sendMessageThrowsSessionNotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        ChatRequest invalidRequest = new ChatRequest(99L, "teste");

        assertThrows(SessionNotFoundException.class, () -> chatService.sendMessage(invalidRequest));
        verify(messageService, never()).createAndSaveMessage(any(), any(), any());
    }

    @Test
    @DisplayName("sendMessage propaga EmbeddingException do RagService")
    void sendMessagePropagatesEmbeddingException() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        MessageResponse userMsg = new MessageResponse(1L, "Minha pergunta", MessageRole.USER, Instant.now());
        when(messageService.createAndSaveMessage(session, MessageRole.USER, "Minha pergunta"))
                .thenReturn(userMsg);

        when(ragServiceProvider.getIfAvailable()).thenReturn(ragService);
        when(ragService.retrieveContext(10L, "Minha pergunta"))
                .thenThrow(new EmbeddingException("Falha no embedding"));

        assertThrows(EmbeddingException.class, () -> chatService.sendMessage(request));
    }

    @Test
    @DisplayName("sendMessage propaga exceção do AiResponseGenerator")
    void sendMessagePropagatesAiResponseException() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        MessageResponse userMsg = new MessageResponse(1L, "Minha pergunta", MessageRole.USER, Instant.now());
        when(messageService.createAndSaveMessage(session, MessageRole.USER, "Minha pergunta"))
                .thenReturn(userMsg);

        when(ragServiceProvider.getIfAvailable()).thenReturn(null);

        when(aiResponseGenerator.generateResponse(eq(10L), eq("Minha pergunta"), anyString()))
                .thenThrow(new RuntimeException("Falha na geração da resposta"));

        assertThrows(RuntimeException.class, () -> chatService.sendMessage(request));
    }
}
