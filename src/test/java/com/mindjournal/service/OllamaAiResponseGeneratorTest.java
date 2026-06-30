package com.mindjournal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mindjournal.config.OllamaGenerationProperties;
import com.mindjournal.exception.GenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class OllamaAiResponseGeneratorTest {

    @Mock
    private RestTemplate restTemplate;

    private OllamaGenerationProperties properties;
    private OllamaAiResponseGenerator generator;

    @BeforeEach
    void setUp() {
        properties = new OllamaGenerationProperties();
        properties.setOllamaUrl("http://localhost:11434/api/chat");
        properties.setModel("llama3.2:3b");
        properties.setTemperature(0.2);
        generator = new OllamaAiResponseGenerator(properties, restTemplate);
    }

    @Test
    @DisplayName("requisição usa a URL configurada")
    void usesConfiguredUrl() {
        var chatResponse = new OllamaAiResponseGenerator.ChatResponse(
            new OllamaAiResponseGenerator.ChatMessage("assistant", "resposta")
        );
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenReturn(ResponseEntity.ok(chatResponse));

        generator.generateResponse(1L, "msg", "");

        @SuppressWarnings("unchecked")
        var captor = ArgumentCaptor.forClass(RequestEntity.class);
        verify(restTemplate).exchange(captor.capture(), eq(OllamaAiResponseGenerator.ChatResponse.class));
        assertTrue(captor.getValue().getUrl().toString().contains("http://localhost:11434/api/chat"));
    }

    @Test
    @DisplayName("requisição usa o modelo configurado")
    void usesConfiguredModel() {
        var chatResponse = new OllamaAiResponseGenerator.ChatResponse(
            new OllamaAiResponseGenerator.ChatMessage("assistant", "resposta")
        );
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenReturn(ResponseEntity.ok(chatResponse));

        generator.generateResponse(1L, "msg", "");

        verify(restTemplate).exchange(argThat(req -> {
            OllamaAiResponseGenerator.ChatRequest body = (OllamaAiResponseGenerator.ChatRequest) ((RequestEntity<?>) req).getBody();
            return body != null && "llama3.2:3b".equals(body.model());
        }), eq(OllamaAiResponseGenerator.ChatResponse.class));
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<RequestEntity<OllamaAiResponseGenerator.ChatRequest>> requestCaptor() {
        return ArgumentCaptor.forClass(RequestEntity.class);
    }

    private OllamaAiResponseGenerator.ChatRequest captureRequest() {
        var captor = requestCaptor();
        var chatResponse = new OllamaAiResponseGenerator.ChatResponse(
            new OllamaAiResponseGenerator.ChatMessage("assistant", "ok")
        );
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenReturn(ResponseEntity.ok(chatResponse));

        generator.generateResponse(1L, "msg", "");
        verify(restTemplate).exchange(captor.capture(), eq(OllamaAiResponseGenerator.ChatResponse.class));
        return (OllamaAiResponseGenerator.ChatRequest) captor.getValue().getBody();
    }

    @Test
    @DisplayName("stream é false na requisição")
    void streamIsFalse() {
        OllamaAiResponseGenerator.ChatRequest request = captureRequest();
        assertFalse(request.stream());
    }

    @Test
    @DisplayName("temperature é enviada na requisição")
    void temperatureIsSent() {
        OllamaAiResponseGenerator.ChatRequest request = captureRequest();
        assertEquals(0.2, request.options().temperature(), 0.0001);
    }

    @Test
    @DisplayName("prompt de sistema está presente")
    void systemPromptIsPresent() {
        OllamaAiResponseGenerator.ChatRequest request = captureRequest();
        assertTrue(request.messages().stream()
            .anyMatch(m -> "system".equals(m.role())));
    }

    @Test
    @DisplayName("pergunta está presente na mensagem do usuário")
    void userMessageIsPresent() {
        var captor = requestCaptor();
        var chatResponse = new OllamaAiResponseGenerator.ChatResponse(
            new OllamaAiResponseGenerator.ChatMessage("assistant", "ok")
        );
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenReturn(ResponseEntity.ok(chatResponse));

        generator.generateResponse(1L, "minha pergunta", "");
        verify(restTemplate).exchange(captor.capture(), eq(OllamaAiResponseGenerator.ChatResponse.class));
        OllamaAiResponseGenerator.ChatRequest request = (OllamaAiResponseGenerator.ChatRequest) captor.getValue().getBody();
        String userContent = request.messages().stream()
            .filter(m -> "user".equals(m.role()))
            .map(OllamaAiResponseGenerator.ChatMessage::content)
            .findFirst().orElse("");
        assertTrue(userContent.contains("minha pergunta"));
    }

    @Test
    @DisplayName("contexto está presente quando informado")
    void contextIsPresentWhenProvided() {
        var captor = requestCaptor();
        var chatResponse = new OllamaAiResponseGenerator.ChatResponse(
            new OllamaAiResponseGenerator.ChatMessage("assistant", "ok")
        );
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenReturn(ResponseEntity.ok(chatResponse));

        generator.generateResponse(1L, "pergunta", "contexto recuperado");
        verify(restTemplate).exchange(captor.capture(), eq(OllamaAiResponseGenerator.ChatResponse.class));
        OllamaAiResponseGenerator.ChatRequest request = (OllamaAiResponseGenerator.ChatRequest) captor.getValue().getBody();
        String userContent = request.messages().stream()
            .filter(m -> "user".equals(m.role()))
            .map(OllamaAiResponseGenerator.ChatMessage::content)
            .findFirst().orElse("");
        assertTrue(userContent.contains("contexto recuperado"));
    }

    @Test
    @DisplayName("contexto vazio não vira null")
    void emptyContextIsNotNull() {
        var captor = requestCaptor();
        var chatResponse = new OllamaAiResponseGenerator.ChatResponse(
            new OllamaAiResponseGenerator.ChatMessage("assistant", "ok")
        );
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenReturn(ResponseEntity.ok(chatResponse));

        generator.generateResponse(1L, "pergunta", "");
        verify(restTemplate).exchange(captor.capture(), eq(OllamaAiResponseGenerator.ChatResponse.class));
        OllamaAiResponseGenerator.ChatRequest request = (OllamaAiResponseGenerator.ChatRequest) captor.getValue().getBody();
        String userContent = request.messages().stream()
            .filter(m -> "user".equals(m.role()))
            .map(OllamaAiResponseGenerator.ChatMessage::content)
            .findFirst().orElse("");
        assertFalse(userContent.contains("null"));
    }

    @Test
    @DisplayName("contexto nulo é tratado como vazio")
    void nullContextIsTreatedAsEmpty() {
        var captor = requestCaptor();
        var chatResponse = new OllamaAiResponseGenerator.ChatResponse(
            new OllamaAiResponseGenerator.ChatMessage("assistant", "ok")
        );
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenReturn(ResponseEntity.ok(chatResponse));

        generator.generateResponse(1L, "pergunta", null);
        verify(restTemplate).exchange(captor.capture(), eq(OllamaAiResponseGenerator.ChatResponse.class));
        OllamaAiResponseGenerator.ChatRequest request = (OllamaAiResponseGenerator.ChatRequest) captor.getValue().getBody();
        String userContent = request.messages().stream()
            .filter(m -> "user".equals(m.role()))
            .map(OllamaAiResponseGenerator.ChatMessage::content)
            .findFirst().orElse("");
        assertFalse(userContent.contains("<CONTEXTO_RECUPERADO>"));
    }

    @Test
    @DisplayName("resposta válida é extraída de message.content")
    void validResponseExtractsContent() {
        var chatResponse = new OllamaAiResponseGenerator.ChatResponse(
            new OllamaAiResponseGenerator.ChatMessage("assistant", "resposta válida")
        );
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenReturn(ResponseEntity.ok(chatResponse));

        String result = generator.generateResponse(1L, "msg", "");
        assertEquals("resposta válida", result);
    }

    @Test
    @DisplayName("falha HTTP gera GenerationException")
    void httpFailureThrowsGenerationException() {
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection refused"));

        assertThrows(GenerationException.class,
            () -> generator.generateResponse(1L, "msg", ""));
    }

    @Test
    @DisplayName("resposta sem message gera GenerationException")
    void responseWithoutMessageThrowsGenerationException() {
        var chatResponse = new OllamaAiResponseGenerator.ChatResponse(null);
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenReturn(ResponseEntity.ok(chatResponse));

        assertThrows(GenerationException.class,
            () -> generator.generateResponse(1L, "msg", ""));
    }

    @Test
    @DisplayName("content vazio gera GenerationException")
    void emptyContentThrowsGenerationException() {
        var chatResponse = new OllamaAiResponseGenerator.ChatResponse(
            new OllamaAiResponseGenerator.ChatMessage("assistant", "")
        );
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenReturn(ResponseEntity.ok(chatResponse));

        assertThrows(GenerationException.class,
            () -> generator.generateResponse(1L, "msg", ""));
    }

    @Test
    @DisplayName("response body nulo gera GenerationException")
    void nullBodyThrowsGenerationException() {
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenReturn(ResponseEntity.ok(null));

        assertThrows(GenerationException.class,
            () -> generator.generateResponse(1L, "msg", ""));
    }

    @Test
    @DisplayName("conteudo do contexto é delimitado por tags")
    void contextIsDelimitedByTags() {
        var captor = requestCaptor();
        var chatResponse = new OllamaAiResponseGenerator.ChatResponse(
            new OllamaAiResponseGenerator.ChatMessage("assistant", "ok")
        );
        when(restTemplate.exchange(any(RequestEntity.class), eq(OllamaAiResponseGenerator.ChatResponse.class)))
            .thenReturn(ResponseEntity.ok(chatResponse));

        generator.generateResponse(1L, "pergunta", "conteudo do documento");
        verify(restTemplate).exchange(captor.capture(), eq(OllamaAiResponseGenerator.ChatResponse.class));
        OllamaAiResponseGenerator.ChatRequest request = (OllamaAiResponseGenerator.ChatRequest) captor.getValue().getBody();
        String userContent = request.messages().stream()
            .filter(m -> "user".equals(m.role()))
            .map(OllamaAiResponseGenerator.ChatMessage::content)
            .findFirst().orElse("");
        assertTrue(userContent.contains("<CONTEXTO_RECUPERADO>"));
        assertTrue(userContent.contains("</CONTEXTO_RECUPERADO>"));
        assertTrue(userContent.contains("conteudo do documento"));
    }
}
