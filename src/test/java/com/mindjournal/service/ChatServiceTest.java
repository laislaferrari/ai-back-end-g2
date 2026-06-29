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
import com.mindjournal.exception.SessionNotFoundException;
import com.mindjournal.repository.SessionRepository;
import com.mindjournal.service.rag.RagContext;
import com.mindjournal.service.rag.RagService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private AiResponseGenerator aiResponseGenerator;

    @Mock
    private ObjectProvider<RagService> ragServiceProvider;

    @Mock
    private RagService ragService;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(sessionRepository, messageService,
                aiResponseGenerator, ragServiceProvider);
    }

    @Test
    @DisplayName("ChatService chama RagService com a sessão correta")
    void callsRagServiceWithCorrectSession() {
        when(ragServiceProvider.getIfAvailable()).thenReturn(ragService);
        Session session = new Session("Teste");
        session.setId(42L);
        when(sessionRepository.findById(42L)).thenReturn(Optional.of(session));
        when(messageService.createAndSaveMessage(eq(session), eq(MessageRole.USER), anyString()))
                .thenReturn(mock(MessageResponse.class));
        when(ragService.retrieveContext(42L, "minha mensagem"))
                .thenReturn(new RagContext("", List.of()));
        when(aiResponseGenerator.generateResponse(42L, "minha mensagem"))
                .thenReturn("resposta");
        when(messageService.createAndSaveMessage(eq(session), eq(MessageRole.ASSISTANT), anyString()))
                .thenReturn(mock(MessageResponse.class));

        ChatResponse response = chatService.sendMessage(new ChatRequest(42L, "minha mensagem"));

        verify(ragService).retrieveContext(42L, "minha mensagem");
        assertNotNull(response.sources());
    }

    @Test
    @DisplayName("ChatService inclui sources na ChatResponse")
    void includesSourcesInResponse() {
        when(ragServiceProvider.getIfAvailable()).thenReturn(ragService);
        Session session = new Session("Teste");
        session.setId(1L);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(messageService.createAndSaveMessage(eq(session), eq(MessageRole.USER), anyString()))
                .thenReturn(mock(MessageResponse.class));

        var source = new SourceDTO(5L, "doc.txt", 10L, "conteudo", 0.87);
        when(ragService.retrieveContext(anyLong(), anyString()))
                .thenReturn(new RagContext("conteudo", List.of(source)));
        when(aiResponseGenerator.generateResponse(anyLong(), anyString()))
                .thenReturn("resposta");
        when(messageService.createAndSaveMessage(eq(session), eq(MessageRole.ASSISTANT), anyString()))
                .thenReturn(mock(MessageResponse.class));

        ChatResponse response = chatService.sendMessage(new ChatRequest(1L, "teste"));

        assertEquals(1, response.sources().size());
        assertEquals(5L, response.sources().get(0).documentId());
        assertEquals(0.87, response.sources().get(0).similarityScore(), 0.0001);
    }

    @Test
    @DisplayName("ChatService funciona sem RagService no profile H2")
    void worksWithoutRagService() {
        when(ragServiceProvider.getIfAvailable()).thenReturn(null);
        Session session = new Session("Teste");
        session.setId(1L);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(messageService.createAndSaveMessage(eq(session), eq(MessageRole.USER), anyString()))
                .thenReturn(mock(MessageResponse.class));
        when(aiResponseGenerator.generateResponse(anyLong(), anyString()))
                .thenReturn("resposta");
        when(messageService.createAndSaveMessage(eq(session), eq(MessageRole.ASSISTANT), anyString()))
                .thenReturn(mock(MessageResponse.class));

        ChatResponse response = chatService.sendMessage(new ChatRequest(1L, "teste"));

        assertTrue(response.sources().isEmpty());
    }

    @Test
    @DisplayName("ChatResponse nunca retorna sources como null")
    void sourcesNeverNull() {
        when(ragServiceProvider.getIfAvailable()).thenReturn(null);
        Session session = new Session("Teste");
        session.setId(1L);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(messageService.createAndSaveMessage(eq(session), eq(MessageRole.USER), anyString()))
                .thenReturn(mock(MessageResponse.class));
        when(aiResponseGenerator.generateResponse(anyLong(), anyString()))
                .thenReturn("resposta");
        when(messageService.createAndSaveMessage(eq(session), eq(MessageRole.ASSISTANT), anyString()))
                .thenReturn(mock(MessageResponse.class));

        ChatResponse response = chatService.sendMessage(new ChatRequest(1L, "teste"));

        assertNotNull(response.sources());
    }

    @Test
    @DisplayName("sessão inexistente lança SessionNotFoundException")
    void sessionNotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(SessionNotFoundException.class,
            () -> chatService.sendMessage(new ChatRequest(99L, "msg")));
    }
}
